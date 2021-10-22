#!/usr/bin/python
# pip install opencv-python
# pip install tensorflow
# pip install keras
# pip install fer
from fer import FER
import matplotlib.pyplot as plt


def emotion(image):
    image_one = plt.imread(image)
    detector = FER(mtcnn=True)
    # Capture all the emotions on the image
    plt.imshow(image_one)
    dominant_emotion, emotion_score = detector.top_emotion(image_one)
    print(dominant_emotion, emotion_score)
    return dominant_emotion


if __name__ == '__main__':
    emotion("bale.jpg")
