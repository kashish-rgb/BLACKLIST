# BLACKLIST — Academic Integrity Review

## Run in VS Code
1. Extract this ZIP completely (do not run files from inside the ZIP preview).
2. In VS Code choose **File → Open Folder** and select the extracted `BLACKLIST` folder (the folder containing this README, `BLACKLIST.jar`, and `ai_image_detector.py`).
3. Run `BLACKLIST.jar`, or open `src/blacklist/BlacklistApp.java` and run it.
4. In **Image Analysis**, click **Add Images…**, then **AI Detector — Analyze Selected Images**.
5. If the app asks you to locate `ai_image_detector.py`, browse to the `ai_image_detector.py` file in the extracted BLACKLIST folder and select it.

## Python setup
The image detector uses the Hugging Face model `capcheck/ai-image-detection` and requires Python packages `transformers`, `torch`, and `Pillow`. The app checks for `.venv\\Scripts\\python.exe` beside the project and then `%USERPROFILE%\\.venv\\Scripts\\python.exe`, before falling back to `python` on PATH.

If needed, activate the virtual environment where packages were installed and run:
```powershell
python -m pip install transformers torch pillow
```

Model predictions and confidence scores are estimates, not proof of image origin. False positives and false negatives are possible. Do not use detector scores alone to make academic misconduct decisions.
