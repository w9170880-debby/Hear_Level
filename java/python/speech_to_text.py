import whisper
import sys

def transcribe_audio(file_path):
    # טעינת המודל (בפעם הראשונה זה עשוי לקחת כמה דקות להורדה)
    model = whisper.load_model("base", device="cpu")    # ביצוע התמלול
    result = model.transcribe(audio=file_path)
    # הדפסת הטקסט - זה מה שחשוב!
    print("--- RESULT START ---")
    print(result["text"])
    print("--- RESULT END ---")

if __name__ == "__main__":
    # בדיקה שהתקבל נתיב לקובץ מה-Java
    if len(sys.argv) > 1:
        transcribe_audio(sys.argv[1])
    else:
        print("Error: No file path provided")