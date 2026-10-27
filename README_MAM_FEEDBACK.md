# BLACKLIST — Mam Feedback Updated

Changes requested by your mam:
- Removed the “AI Detector — Analyze Selected Images” button from the Images tab.
- Removed “Lexical diversity” from the multiple-file writing-pattern results.
- The writing-pattern result now displays “Average words per sentence” followed by an “AI Detection” status.

## Important note
The displayed AI Detection status is intentionally **INCONCLUSIVE**. Average sentence length and other simple writing statistics are not a reliable AI detector and cannot prove whether text was written by AI.

## Open in VS Code
1. Extract this ZIP.
2. Open the extracted `BLACKLIST` folder in VS Code.
3. The updated Java source is `src/blacklist/BlacklistApp.java`.

## Recompile (requires a JDK)
Open the terminal in the `BLACKLIST` folder and run:

```powershell
javac -encoding UTF-8 -d out src\blacklist\BlacklistApp.java
java -cp out blacklist.BlacklistApp
```

The included `BLACKLIST.jar` and `out` class files are retained from the original project and may not contain these source changes until you recompile. Run the commands above to test the updated code.
