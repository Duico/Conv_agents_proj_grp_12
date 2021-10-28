#!/usr/bin/env python
# coding: utf-8

# In[23]:


from nltk.sentiment import SentimentIntensityAnalyzer
from http.server import BaseHTTPRequestHandler, HTTPServer


# In[28]:


hostName = "localhost"
serverPort = 9998
sia = SentimentIntensityAnalyzer()


# In[29]:


def detectSentiment(userResponse):
    global sia
    dic = sia.polarity_scores(userResponse.replace('_',' '))
    dic_keys = list(dic.keys())
    max_ele = dic_keys[0]
    max_val = dic[dic_keys[0]]
    for key in dic_keys:
        if dic[key] > max_val:
            max_ele = key
            max_val = dic[key]
    return max_ele


# In[30]:


class MyServer(BaseHTTPRequestHandler):
    def do_GET(self):
        sentiment = detectSentiment(self.path[1:])
        self.send_response(200,sentiment)
        self.send_header("Content-type", "text/html")
        self.end_headers()
        self.wfile.write(bytes("<html><head><title>https://pythonbasics.org</title></head>", "utf-8"))
        self.wfile.write(bytes("<p>Request: %s</p>" % self.path, "utf-8"))
        self.wfile.write(bytes("<body>", "utf-8"))
        self.wfile.write(bytes("<p></p>", "utf-8"))
        self.wfile.write(bytes("</body></html>", "utf-8"))


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


# In[2]:





# In[ ]:




