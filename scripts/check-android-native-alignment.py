"""Verify Android 16 KB native ELF segment alignment; zip alignment is checked by zipalign separately."""
import argparse
import struct
import zipfile

parser=argparse.ArgumentParser()
parser.add_argument('apk')
args=parser.parse_args()
checked=0
with zipfile.ZipFile(args.apk) as archive:
    for name in archive.namelist():
        if not name.endswith('.so') or not name.startswith('lib/'):
            continue
        data=archive.read(name)
        if data[:4]!=b'\x7fELF' or data[5]!=1:
            raise ValueError(f'Unsupported ELF: {name}')
        is64=data[4]==2
        offset=struct.unpack_from('<Q' if is64 else '<I',data,32 if is64 else 28)[0]
        entry_size,count=struct.unpack_from('<HH',data,54 if is64 else 42)
        for i in range(count):
            entry=offset+i*entry_size
            if struct.unpack_from('<I',data,entry)[0]!=1:
                continue
            if is64:
                file_offset,address=struct.unpack_from('<QQ',data,entry+8)
                alignment=struct.unpack_from('<Q',data,entry+48)[0]
            else:
                file_offset,address=struct.unpack_from('<II',data,entry+4)
                alignment=struct.unpack_from('<I',data,entry+28)[0]
            if alignment<16384 or (file_offset-address)%16384:
                raise ValueError(f'Native library is not 16 KB aligned: {name}')
        checked+=1
print(f'PASS: {checked} native libraries have 16 KB-aligned load segments')
