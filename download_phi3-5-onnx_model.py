#!/usr/bin/env python3
"""Download a CPU INT4 Phi-3.5 ONNX build for PrOOPt's local runtime.

Discovers the correct subfolder instead of hardcoding it.
Requires:  pip install -U huggingface_hub
"""
import shutil
from pathlib import Path

from huggingface_hub import HfApi, snapshot_download
from huggingface_hub.utils import RepositoryNotFoundError

REPO_ID = "microsoft/Phi-3.5-mini-instruct-onnx"
DEST = Path("models/phi-3.5-mini-onnx")


def pick_model_dir(files: list[str]) -> str:
    """Choose the folder holding a CPU INT4 model.onnx, with sensible fallbacks."""
    onnx_dirs = sorted({f.rsplit("/", 1)[0] if "/" in f else ""
                        for f in files if f.endswith("model.onnx")})
    if not onnx_dirs:
        raise SystemExit("No 'model.onnx' found in the repo file list.")

    def score(d: str) -> tuple:
        low = d.lower()
        return ("cpu" in low, "int4" in low, "mobile" not in low)

    best = max(onnx_dirs, key=score)
    print("Candidate model folders:")
    for d in onnx_dirs:
        print(f"  {'-> ' if d == best else '   '}{d or '(repo root)'}")
    return best


def main() -> None:
    api = HfApi()
    try:
        files = api.list_repo_files(REPO_ID)
    except RepositoryNotFoundError:
        raise SystemExit(
            f"Repo '{REPO_ID}' not found or gated. "
            f"Run 'huggingface-cli login' if it's gated, or check the id at "
            f"https://huggingface.co/models?search=phi-3.5%20onnx"
        )

    model_dir = pick_model_dir(files)
    pattern = f"{model_dir}/*" if model_dir else "*"

    DEST.mkdir(parents=True, exist_ok=True)
    staging = DEST / "_hf"
    print(f"\nDownloading '{pattern}' from {REPO_ID} …")
    snapshot_download(repo_id=REPO_ID, allow_patterns=[pattern], local_dir=staging)

    # Flatten the discovered subfolder up into DEST.
    src = staging / model_dir if model_dir else staging
    for item in src.iterdir():
        if item.name == "_hf":
            continue
        target = DEST / item.name
        if target.exists():
            target.unlink() if target.is_file() else shutil.rmtree(target)
        shutil.move(str(item), str(target))
    shutil.rmtree(staging, ignore_errors=True)

    print(f"\nDone. Files in {DEST.resolve()}:")
    for f in sorted(DEST.iterdir()):
        if f.is_file():
            print(f"  {f.name:32s} {f.stat().st_size / 1e6:8.1f} MB")

    needed = ["model.onnx", "tokenizer.json"]
    missing = [n for n in needed if not (DEST / n).is_file()]
    if missing:
        print(f"\nWARNING: required files missing: {missing}")
    else:
        print("\nPoint PrOOPt at it (application-*.yml):")
        print(f"  prooopt.models.local.model-path:     {DEST}/model.onnx")
        print(f"  prooopt.models.local.tokenizer-path: {DEST}/tokenizer.json")


if __name__ == "__main__":
    main()