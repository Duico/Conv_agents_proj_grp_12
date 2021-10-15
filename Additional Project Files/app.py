import os
os.environ["CUDA_VISIBLE_DEVICES"] = "0"
from flask import Flask, Response
import cv2
import videocamera as vc
import numpy as np

app = Flask(__name__)

video_camera = vc.VideoCamera()

def gen(camera):
    emotion_i = []
    ret, img = camera.get_frame()
    # TODO: use image to obtain emotion, return emotion.

@app.route('/emotion')
def ret_emotion():
    return Response(gen(video_camera))


if __name__ == '__main__':
    app.run()
