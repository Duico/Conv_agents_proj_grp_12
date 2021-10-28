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
time_series_emotion = []


# In[3]:


cam = cv2.VideoCapture(0) #0=front-cam, 1=back-cam
cam.set(cv2.CAP_PROP_FRAME_WIDTH, 1300)
cam.set(cv2.CAP_PROP_FRAME_HEIGHT, 1500)


# In[4]:


class MyServer(BaseHTTPRequestHandler):
    aggregate_emo = "neutral"
    def do_GET(self):
        global time_series_emotion
        emotion = detectEmotion()
        if(len(time_series_emotion) == 0 and emotion != []):
            time_series_emotion = emotion

        elif(len(emotion) != 0):
            for key in time_series_emotion.keys():
                time_series_emotion[key] = 0.9 * emotion[key] + 0.1 * time_series_emotion[key]
        
        if(len(time_series_emotion) != 0):
            dict_emo = list(time_series_emotion.keys())
            aggregate_emo = dict_emo[0]
            aggregate_val = time_series_emotion[aggregate_emo]
            for key in time_series_emotion.keys():
                temp = time_series_emotion[key]
                if(temp > aggregate_val):
                    aggregate_val = temp
                    aggregate_emo = key
        self.send_response(200,aggregate_emo)
        self.send_header("Content-type", "text/html")
        self.end_headers()
        self.wfile.write(bytes("<html><head><title>https://pythonbasics.org</title></head>", "utf-8"))
        self.wfile.write(bytes("<p>Request: %s</p>" % self.path, "utf-8"))
        self.wfile.write(bytes("<body>", "utf-8"))
        self.wfile.write(bytes("<p></p>", "utf-8"))
        self.wfile.write(bytes("</body></html>", "utf-8"))


# In[5]:


def detectEmotion():
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
    #plt.imshow(image_one)
    dominant_emotion, emotion_score = detector.top_emotion(image_one)
    emotions = detector.detect_emotions(image_one)
    if(len(emotions) == 0):
        return []
    return emotions[0]["emotions"] #dominant_emotion


# In[7]:


emo = detectEmotion()
if(len(time_series_emotion) == 0):
            time_series_emotion = emo

elif(len(emo)!= 0):
    for key in time_series_emotion.keys():
        time_series_emotion[key] = 0.9 * emo[key] + 0.1 * time_series_emotion[key]
else:
    pass
    


# In[8]:


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




