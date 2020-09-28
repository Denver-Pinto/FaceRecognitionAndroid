# FaceRecognitionAndroid
This repository contains a demonstration of face recognition using the FaceNet network (https://arxiv.org/pdf/1503.03832.pdf) through an Android application.
Reference to understand model :- https://www.freecodecamp.org/news/making-your-own-face-recognition-system-29a8e728107c/
Model weights and code referred from :- https://github.com/Skuldur/facenet-face-recognition

#Steps to run application
1. Run colab file to get estimator_model folder
2. Deploy an Azure VM instance (NVIDIA GPU-Optimized Image for TensorFlow)
3. Transfer folder to instance using scp
4. ssh into it using ipaddr 
5. run command :- docker pull tensorflow/serving:latest-gpu
6. run command :- docker run --gpus all -p 8501:8501   --mount type=bind,source=/path/to/estimator_model/,target=/models/estimator_model   -e MODEL_NAME=estimator_model -t tensorflow/serving:latest-gpu
7. check :- http://ipaddr:8501/v1/models/estimator_model
8. Download the android folders and run the application 
