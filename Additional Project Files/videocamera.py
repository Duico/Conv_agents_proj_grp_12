import cv2
import numpy as np

face_cascade = cv2.CascadeClassifier('./haarcascade_frontalface_default.xml')


class VideoCamera(object):
    def __init__(self):
        # capturing video
        self.video = cv2.VideoCapture(0)

    def __del__(self):
        # releasing camera
        self.video.release()

    def get_frame(self):
        # extracting frames
        succes = False
        ret, frame = self.video.read()
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        face_rects = face_cascade.detectMultiScale(gray, 1.3, 5)
        max_x, max_y, max_w, max_h = 0, 0, 0, 0
        for (x, y, w, h) in face_rects:
            if w > max_w and h > max_h:
                max_x, max_y, max_w, max_h = x, y, w, h
        if max_w > 0 and max_h > 0:
            frame = gray[max_y:max_y + max_h, max_x:max_x + max_w]
            succes = True
        return succes, frame
