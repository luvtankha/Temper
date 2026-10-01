"""Synthetic tests only: no Docker, DNS, HTTP, real credentials or host initialization."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('feedback_deploy', Path(__file__).with_name('feedback-deploy.py'))
deploy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deploy)


class DeploymentTests(unittest.TestCase):
    def sample(self):
        # Fictional syntax fixture only, never used to start a service or perform DNS lookup.
        return {'FEEDBACK_DOMAIN': 'feedback.publisher-fixture.in', 'ACME_EMAIL': 'fixture@publisher-fixture.in',
                'CADDY_IMAGE': 'caddy:2@sha256:' + 'a' * 64, 'FEEDBACK_ROOT': '/srv/temper-feedback'}

    def test_placeholder_and_injection_configuration_rejected(self):
        deploy.validate_configuration(self.sample())
        for field, bad in [('FEEDBACK_DOMAIN', ''), ('FEEDBACK_DOMAIN', 'feedback.example.com'),
                           ('FEEDBACK_DOMAIN', '127.0.0.1'), ('FEEDBACK_DOMAIN', 'localhost'),
                           ('FEEDBACK_DOMAIN', 'https://feedback.publisher-fixture.in'),
                           ('FEEDBACK_DOMAIN', 'host.in\nrespond 200'), ('ACME_EMAIL', 'bad contact'),
                           ('CADDY_IMAGE', 'caddy:latest'), ('FEEDBACK_ROOT', '/srv/../private')]:
            values = self.sample()
            values[field] = bad
            with self.subTest(field=field, bad=bad), self.assertRaises(ValueError):
                deploy.validate_configuration(values)

    def test_config_not_sourced_and_no_unknown_or_duplicate_fields(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'fixture.env'
            text = '\n'.join(key + '=' + value for key, value in self.sample().items())
            path.write_text(text, encoding='utf-8')
            self.assertEqual(deploy.configuration(path), self.sample())
            for suffix in ['\nFEEDBACK_ROOT=/srv/temper-feedback', '\nEVIL=$(echo forbidden)']:
                path.write_text(text + suffix, encoding='utf-8')
                with self.assertRaises(ValueError):
                    deploy.configuration(path)

    def test_random_key_exclusive_and_invalid_file_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'synthetic.key'
            deploy.create_key(path)
            deploy.validate_key(path)
            original = path.read_bytes()
            with self.assertRaises(FileExistsError):
                deploy.create_key(path)
            self.assertEqual(path.read_bytes(), original)
            path.write_bytes(b'not-base64!')
            with self.assertRaises(ValueError):
                deploy.validate_key(path)

    def test_containment_static_invariants(self):
        deploy.check_kit()


if __name__ == '__main__':
    unittest.main()
