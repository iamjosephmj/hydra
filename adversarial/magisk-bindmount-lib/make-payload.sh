#!/usr/bin/env bash
# Build payload/libdicore.so from a shipped/trail libdicore.so.
#   MODE=trip     (default) flip one aligned .text NOP -> YIELD: functional no-op, but
#                 the bytes no longer match the build-baked hash -> G2 should trip.
#   MODE=retpatch FILEOFF=<hex>  overwrite 4 bytes at a FILE offset with aarch64 RET
#                 (0xC0035FD6 little-endian) — neuter a function ON DISK from load time.
#                 Derive FILEOFF from a symbol vaddr with:  readelf -lW <so>  (map the
#                 target PT_LOAD: fileoff = vaddr - p_vaddr + p_offset).
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
SRC="${1:?usage: make-payload.sh <src libdicore.so>}"; MODE="${MODE:-trip}"
mkdir -p "$HERE/payload"; OUT="$HERE/payload/libdicore.so"
python3 - "$SRC" "$OUT" "$MODE" "${FILEOFF:-0}" <<'PY'
import sys
src,out,mode,foff = sys.argv[1],sys.argv[2],sys.argv[3],int(sys.argv[4],16) if sys.argv[4].startswith("0x") else int(sys.argv[4] or "0")
d=bytearray(open(src,"rb").read())
if mode=="retpatch":
    if not foff: raise SystemExit("retpatch needs FILEOFF=<hex>")
    d[foff:foff+4]=bytes.fromhex("c0035fd6")  # RET (little-endian)
    print(f"ret-patched file offset 0x{foff:x} -> {out}")
else:
    NOP=bytes.fromhex("1f2003d5"); i=0x60000
    while i<len(d):
        j=d.find(NOP,i)
        if j<0: raise SystemExit("no aligned .text NOP to flip")
        if j%4==0: d[j]=0x3f; print(f"flipped NOP@0x{j:x} (NOP->YIELD) -> {out}"); break
        i=j+1
open(out,"wb").write(d)
PY
