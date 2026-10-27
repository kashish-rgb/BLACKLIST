"""BLACKLIST image AI-classification bridge.
Usage: python ai_image_detector.py image1.png image2.jpg ...
The model's predictions are fallible and must not be treated as proof.
"""
import os
import sys

MODEL_NAME = "capcheck/ai-image-detection"


def main():
    paths = sys.argv[1:]
    if not paths:
        print("No image paths were supplied.")
        return 2
    try:
        from PIL import Image
        from transformers import pipeline
    except Exception as exc:
        print(f"Missing Python dependency: {exc}")
        return 3

    try:
        classifier = pipeline("image-classification", model=MODEL_NAME)
    except Exception as exc:
        print(f"Could not load model {MODEL_NAME}: {exc}")
        return 4

    for path in paths:
        name = os.path.basename(path)
        try:
            with Image.open(path) as source:
                image = source.convert("RGB")
            predictions = classifier(image, top_k=3)
            print(f"FILE: {name}")
            for item in predictions:
                print(f"  {item['label']}: {item['score'] * 100:.2f}% confidence")
            print("  Note: model prediction only; not proof of image origin.")
        except Exception as exc:
            print(f"FILE: {name}\n  ERROR: {exc}")
        print()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
