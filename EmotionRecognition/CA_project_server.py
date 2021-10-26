#!/usr/bin/env python
# coding: utf-8

# In[1]:


from fer import FER
import matplotlib.pyplot as plt
import cv2
from http.server import BaseHTTPRequestHandler, HTTPServer


# In[2]:


hostName = "localhost"
serverPort = 9999
count = 0


# In[3]:


cam = cv2.VideoCapture(0) #0=front-cam, 1=back-cam
cam.set(cv2.CAP_PROP_FRAME_WIDTH, 1300)
cam.set(cv2.CAP_PROP_FRAME_HEIGHT, 1500)


# In[4]:


class MyServer(BaseHTTPRequestHandler):
    def do_GET(self):
        emotion = detectEmotion()
        print(emotion)
        self.send_response(200,emotion)
        self.send_header("Content-type", "text/html")
        self.end_headers()
        self.wfile.write(bytes("<html><head><title>https://pythonbasics.org</title></head>", "utf-8"))
        self.wfile.write(bytes("<p>Request: %s</p>" % self.path, "utf-8"))
        self.wfile.write(bytes("<body>", "utf-8"))
        self.wfile.write(bytes("<p></p>", "utf-8"))
        self.wfile.write(bytes("</body></html>", "utf-8"))


# In[5]:


def detectEmotion():
    detectedEmo = "" 
    ret, img = cam.read()    ## predict yolo
    image_context = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    emo = emotion(image_context)
    return emo


# In[6]:


def emotion(image):
    #image_context = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    #image_one = plt.imread(image)
    image_one = image
    detector = FER(mtcnn=True)
    # Capture all the emotions on the image
    plt.imshow(image_one)
    dominant_emotion, emotion_score = detector.top_emotion(image_one)
    emotions = detector.detect_emotions(image_one)
    return dominant_emotion


# In[7]:


emo = detectEmotion()
print(emo)


# In[ ]:


webServer = HTTPServer((hostName, serverPort), MyServer)
print("Server started http://%s:%s" % (hostName, serverPort))

try:
    webServer.serve_forever()
except KeyboardInterrupt:
    pass

webServer.server_close()
cam.release()
print("Server stopped.")


# In[ ]:





# In[ ]:





# In[ ]:




