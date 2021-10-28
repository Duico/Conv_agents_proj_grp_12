#!/usr/bin/env python
# coding: utf-8

from fer import FER
import cv2
from http.server import SimpleHTTPRequestHandler, HTTPServer
from nltk.sentiment import SentimentIntensityAnalyzer
import json


hostName = "localhost"
serverPort = 9999
count = 0
time_series_emotion = []

cam = cv2.VideoCapture(0) #0=front-cam, 1=back-cam
cam.set(cv2.CAP_PROP_FRAME_WIDTH, 1300)
cam.set(cv2.CAP_PROP_FRAME_HEIGHT, 1500)

sia = SentimentIntensityAnalyzer()


class MyServer(SimpleHTTPRequestHandler):
    aggregate_emo = "neutral"

    def do_GET(self):
        if self.path == '/text-sentiment':
            text = json.loads(self.rfile.read(int(self.headers["Content-Length"])))['text']
            sentiment = detectSentiment(text)
            self.send_response(200, sentiment)
        else:
            global time_series_emotion
            current_emotion = detectEmotion()
            if len(time_series_emotion) == 0 and current_emotion != []:
                time_series_emotion = current_emotion

            elif len(current_emotion) != 0:
                for emotion_key in time_series_emotion.keys():
                    time_series_emotion[emotion_key] = (0.9 * current_emotion[emotion_key] + 0.1 *
                                                        time_series_emotion[emotion_key])

            if len(time_series_emotion) != 0:
                dict_emo = list(time_series_emotion.keys())
                self.aggregate_emo = dict_emo[0]
                aggregate_val = time_series_emotion[self.aggregate_emo]
                for emotion_key in time_series_emotion.keys():
                    temp = time_series_emotion[emotion_key]
                    if temp > aggregate_val:
                        aggregate_val = temp
                        self.aggregate_emo = emotion_key
            self.send_response(200, self.aggregate_emo)
        self.send_header("Content-type", "text/html")
        self.end_headers()
        self.wfile.write(bytes("<html><head><title>MathTutor Emotion Server</title></head>", "utf-8"))
        self.wfile.write(bytes("<p>Request: %s</p>" % self.path, "utf-8"))
        self.wfile.write(bytes("<body>", "utf-8"))
        self.wfile.write(bytes("<p></p>", "utf-8"))
        self.wfile.write(bytes("</body></html>", "utf-8"))


def detectSentiment(text):
    res = sia.polarity_scores(text)
    return res


def detectEmotion():
    ret, img = cam.read()
    if not ret:
        print("Could not read from camera.")
        return list()
    image_context = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    emo = emotion(image_context)
    return emo


def emotion(image):
    image_one = image
    detector = FER(mtcnn=True)
    # Capture all the emotions on the image
    emotions = detector.detect_emotions(image_one)
    if len(emotions) == 0:
        return []
    return emotions[0]["emotions"]  # dominant_emotion


def main():
    global time_series_emotion
    emo = detectEmotion()
    if len(time_series_emotion) == 0:
        time_series_emotion = emo

    elif len(emo) != 0:
        for key in time_series_emotion.keys():
            time_series_emotion[key] = 0.9 * emo[key] + 0.1 * time_series_emotion[key]
    else:
        pass

    web_server = HTTPServer((hostName, serverPort), MyServer)
    print("Server started http://%s:%s" % (hostName, serverPort))

    try:
        web_server.serve_forever()
    except KeyboardInterrupt:
        pass

    web_server.server_close()
    cam.release()
    print("Server stopped.")


if __name__ == "__main__":
    main()
