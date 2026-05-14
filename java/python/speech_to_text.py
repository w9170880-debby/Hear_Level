import whisper
import sys

def transcribe_audio(file_path):
    model = whisper.load_model("base", device="cpu")
    result = model.transcribe(audio=file_path)

    # חישוב מהירות דיבור — מילים לדקה
    # Whisper מחזיר segments עם timestamps מדויקים לכל קטע דיבור
    # משתמשים בזמן תחילת הסגמנט הראשון עד סוף האחרון (לא כולל שתיקות ארוכות בהתחלה/סוף)
    segments = result.get("segments", [])
    if segments:
        duration_seconds = segments[-1]["end"] - segments[0]["start"]
        word_count = len(result["text"].split())
        wpm = round((word_count / duration_seconds) * 60, 2) if duration_seconds > 0 else 0.0
    else:
        wpm = 0.0

    print("--- RESULT START ---")
    print(result["text"])
    print("--- RESULT END ---")
    print("--- WPM START ---")
    print(wpm)
    print("--- WPM END ---")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        transcribe_audio(sys.argv[1])
    else:
        print("Error: No file path provided")
