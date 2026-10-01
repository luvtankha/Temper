#!/usr/bin/env python3
"""Single-host feedback deployment; never prints key contents or sources a shell env file."""
import argparse
import base64
import ipaddress
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import stat
import subprocess
import sys

KIT = Path(__file__).resolve().parent
ROOT = Path('/srv/temper-feedback')
FIELDS = {'FEEDBACK_DOMAIN', 'ACME_EMAIL', 'CADDY_IMAGE', 'FEEDBACK_ROOT'}


def configuration(path):
    values = {}
    for line in Path(path).read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        key, separator, value = line.partition('=')
        if not separator or key not in FIELDS or key in values:
            raise ValueError('Configuration needs unique, recognized KEY=value entries')
        values[key] = value
    validate_configuration(values)
    return values


def validate_configuration(values):
    if set(values) != FIELDS or any(not value for value in values.values()):
        raise ValueError('Set a real FEEDBACK_DOMAIN, ACME_EMAIL, CADDY_IMAGE and FEEDBACK_ROOT first')
    domain = values['FEEDBACK_DOMAIN']
    labels = domain.split('.')
    if (len(domain) > 253 or len(labels) < 2 or domain != domain.lower()
            or any(not re.fullmatch(r'[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?', label) for label in labels)
            or not re.fullmatch(r'[a-z]{2,63}', labels[-1])):
        raise ValueError('FEEDBACK_DOMAIN must be one lowercase public DNS hostname, without URL or port')
    if (domain in {'example.com', 'example.org', 'example.net'}
            or any(domain.endswith('.' + name) for name in ['example.com', 'example.org', 'example.net'])
            or labels[-1] in {'invalid', 'test', 'localhost', 'local', 'example'}):
        raise ValueError('Use your actual public domain, not a reserved/example hostname')
    try:
        ipaddress.ip_address(domain)
    except ValueError:
        pass
    else:
        raise ValueError('A public DNS hostname is required')
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.+%-]*@[A-Za-z0-9.-]+\.[A-Za-z]{2,63}', values['ACME_EMAIL']):
        raise ValueError('ACME_EMAIL must be the publisher certificate contact')
    if not re.fullmatch(r'caddy:2(?:\.[0-9]+){0,2}(?:-alpine)?@sha256:[0-9a-f]{64}', values['CADDY_IMAGE']):
        raise ValueError('CADDY_IMAGE needs a reviewed official caddy:2 image with immutable sha256 digest')
    if values['FEEDBACK_ROOT'] != ROOT.as_posix():
        raise ValueError('This kit uses only /srv/temper-feedback; arbitrary storage paths are refused')


def no_links(path):
    path = Path(path)
    for candidate in [path, *path.parents]:
        if candidate.is_symlink():
            raise ValueError('Deployment paths must not contain symbolic links')


def require_host():
    if sys.platform != 'linux' or os.geteuid() != 0:
        raise ValueError('Host initialization/preflight require Linux and root; no local software is installed')
    no_links(ROOT)


def private_directory(path, owner):
    no_links(path)
    path.mkdir(mode=0o700, exist_ok=True)
    if not path.is_dir():
        raise ValueError('A required private directory is unavailable')
    os.chown(path, owner, owner)
    os.chmod(path, 0o700)


def create_key(path):
    """Exclusive creation, never overwrite an encryption key or print its value."""
    no_links(path)
    flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, 'O_NOFOLLOW', 0)
    descriptor = os.open(path, flags, 0o600)
    try:
        with os.fdopen(descriptor, 'wb') as stream:
            stream.write(base64.b64encode(secrets.token_bytes(32)) + b'\n')
            stream.flush()
            os.fsync(stream.fileno())
    except BaseException:
        # Preserve even a partial file rather than generating a new key over an existing path.
        raise


def validate_key(path):
    no_links(path)
    if not path.is_file() or not 44 <= path.stat().st_size <= 100:
        raise ValueError('Storage key file is missing or invalid; do not replace an existing key')
    try:
        decoded = base64.b64decode(path.read_bytes().strip(), validate=True)
    except (ValueError, base64.binascii.Error):
        raise ValueError('Storage key file is invalid; no contents were printed') from None
    if len(decoded) != 32:
        raise ValueError('Storage key must contain Base64 of exactly 32 random bytes')


def initialize():
    require_host()
    private_directory(ROOT, 0)
    private_directory(ROOT / 'records', 10001)
    private_directory(ROOT / 'secrets', 0)
    private_directory(ROOT / 'caddy-data', 10101)
    private_directory(ROOT / 'caddy-config', 10101)
    key = ROOT / 'secrets/feedback-storage.key'
    if not key.exists():
        create_key(key)
    validate_key(key)
    # File-based Compose secrets retain host ownership/mode. Compose cannot remap them.
    os.chown(key, 10001, 10001)
    os.chmod(key, 0o400)
    print('Private directories and storage key ready. No service started; key contents were not printed.')


def command(args, values, capture=False):
    environment = os.environ.copy()
    environment.update(values)
    return subprocess.run(args, cwd=KIT, env=environment, check=True,
                          capture_output=capture, text=True)


def docker_command(config):
    return ['docker', 'compose', '--project-name', 'temper-feedback', '--env-file',
            str(Path(config).resolve()), '-f', str(KIT / 'compose.yaml')]


def preflight(config, values):
    require_host()
    for path, owner, mode in [(ROOT, 0, 0o700), (ROOT / 'records', 10001, 0o700),
                              (ROOT / 'secrets', 0, 0o700), (ROOT / 'caddy-data', 10101, 0o700),
                              (ROOT / 'caddy-config', 10101, 0o700),
                              (ROOT / 'secrets/feedback-storage.key', 10001, 0o400)]:
        no_links(path)
        info = path.stat()
        if info.st_uid != owner or stat.S_IMODE(info.st_mode) != mode:
            raise ValueError('Private deployment ownership/permissions differ; run --init on the actual VPS')
    validate_key(ROOT / 'secrets/feedback-storage.key')
    if not shutil.which('docker'):
        raise ValueError('Docker Engine and Compose v2 must already be installed on the actual VPS')
    if os.environ.get('DOCKER_HOST') or os.environ.get('DOCKER_CONTEXT'):
        raise ValueError('Use the local default Docker daemon, without DOCKER_HOST or DOCKER_CONTEXT')
    docker_context = command(['docker', 'context', 'show'], values, capture=True).stdout.strip()
    if docker_context != 'default':
        raise ValueError('This single-host kit requires the local default Docker context')
    endpoint = command(['docker', 'context', 'inspect', 'default', '--format',
                        '{{.Endpoints.docker.Host}}'], values, capture=True).stdout.strip()
    if endpoint != 'unix:///var/run/docker.sock':
        raise ValueError('This kit requires the local /var/run/docker.sock daemon, not a remote/custom endpoint')
    security = command(['docker', 'info', '--format', '{{json .SecurityOptions}}'], values, capture=True).stdout
    if 'rootless' in security or 'userns' in security:
        raise ValueError('This kit requires default UID mapping; adapt and verify permissions before rootless/userns use')
    version = command(['docker', 'compose', 'version', '--short'], values, capture=True).stdout.strip().lstrip('v')
    match = re.match(r'(\d+)\.(\d+)\.(\d+)', version)
    if not match or tuple(map(int, match.groups())) < (2, 24, 0):
        raise ValueError('Docker Compose 2.24 or newer is required')
    command(docker_command(config) + ['config', '--quiet'], values, capture=True)
    print('Host permissions, immutable Caddy reference and Compose configuration passed preflight.')
    print('DNS, firewall, TLS issuance, published privacy policy and external abuse controls require operator verification.')


def check_kit():
    """Portable static check; is not Docker/Caddy schema or runtime validation."""
    compose = (KIT / 'compose.yaml').read_text(encoding='utf-8')
    caddy = (KIT / 'Caddyfile').read_text(encoding='utf-8')
    docker = (KIT / 'Dockerfile').read_text(encoding='utf-8')
    for required in ['user: "10001:10001"', 'user: "10101:10101"', 'internal: true',
                     'create_host_path: false', 'read_only: true', 'condition: service_healthy']:
        if required not in compose:
            raise ValueError('Deployment containment invariant is missing')
    feedback_service = compose.split('  caddy:', 1)[0]
    if 'ports:' in feedback_service or 'privileged:' in compose or 'network_mode:' in compose:
        raise ValueError('The feedback container must not be exposed or privileged')
    if not all(value in caddy for value in ['output discard', 'admin off', 'max_size 384000',
                                           '/api/learning/sessions', '/api/learning/contributions', 'respond 404']):
        raise ValueError('The restricted HTTPS boundary is incomplete')
    if 'USER 10001:10001' not in docker or 'FeedbackHealth.java' not in docker:
        raise ValueError('Non-root feedback image or local health check is missing')
    print('Portable static kit checks passed. Docker/Caddy build and runtime verification remain pending on Linux.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    actions = parser.add_mutually_exclusive_group(required=True)
    actions.add_argument('--check', action='store_true', help='portable static checks, no secrets or Docker required')
    actions.add_argument('--init', action='store_true', help='create private directories/key on actual Linux VPS; never start')
    actions.add_argument('--preflight', action='store_true', help='validate actual host and Compose without starting services')
    actions.add_argument('--start', action='store_true', help='preflight, validate Caddy, build and start one service instance')
    parser.add_argument('--config', default='/etc/temper-feedback.env')
    args = parser.parse_args()
    try:
        if args.check:
            check_kit()
            return
        values = configuration(args.config)
        if args.init:
            initialize()
            return
        preflight(args.config, values)
        if args.start:
            compose = docker_command(args.config)
            command(compose + ['run', '--rm', '--no-deps', 'caddy', 'caddy', 'validate', '--config',
                                '/etc/caddy/Caddyfile', '--adapter', 'caddyfile'], values)
            command(compose + ['up', '-d', '--build', '--scale', 'feedback=1', '--wait', '--wait-timeout', '180'], values)
            print('Containers started. Check public HTTPS and fictional submission/deletion before enabling Android feedback.')
    except (OSError, ValueError, subprocess.CalledProcessError):
        # Configuration errors may name values; never echo subprocess stderr or secret file content.
        error = sys.exc_info()[1]
        if isinstance(error, ValueError):
            print('Preflight stopped: ' + str(error), file=sys.stderr)
        else:
            print('Preflight stopped: missing/unavailable file, host tool or failed validation; no secrets printed.', file=sys.stderr)
        raise SystemExit(1)


if __name__ == '__main__':
    main()
