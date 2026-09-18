# -*- coding: utf-8 -*-
"""批量更新 H5 静态资源 ?v= 版本号（微信缓存刷新用）。

用法（在仓库根目录）：
  python scripts/bump_h5_asset_ver.py
  python scripts/bump_h5_asset_ver.py 20260320

不传参数时使用当天日期 YYYYMMDD。
只改 server-api/.../static/h5/**/*.html 中 /h5/assets/ 的 css/js/图片引用。
"""
from __future__ import print_function

import pathlib
import re
import sys
from datetime import date

ROOT = pathlib.Path(__file__).resolve().parents[1]
H5_ROOT = ROOT / "server-api" / "src" / "main" / "resources" / "static" / "h5"

# 已有 ?v= 或尚无版本号的资源 URL
PAT = re.compile(
    r'(/h5/assets/[^"\'?\s]+\.(?:css|js|png|jpg|jpeg|gif|webp|svg))(?:\?v=[^"\']*)?(?=["\'])'
)


def main(argv):
    if len(argv) > 1 and argv[1].strip():
        ver = argv[1].strip()
    else:
        ver = date.today().strftime("%Y%m%d")

    if not H5_ROOT.is_dir():
        print("H5 目录不存在:", H5_ROOT)
        return 1

    updated = 0
    for path in sorted(H5_ROOT.rglob("*.html")):
        raw = path.read_bytes()
        if raw.startswith(b"\xef\xbb\xbf"):
            text = raw[3:].decode("utf-8")
        else:
            text = raw.decode("utf-8")

        new = PAT.sub(r"\1?v=" + ver, text)
        if new == text:
            print("skip ", path.relative_to(ROOT))
            continue
        path.write_bytes(new.encode("utf-8"))
        updated += 1
        print("ok   ", path.relative_to(ROOT))

    print("version=%s, files_updated=%d" % (ver, updated))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
