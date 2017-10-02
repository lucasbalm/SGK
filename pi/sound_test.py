import pygame
import time
from time import sleep

pygame.mixer.init()
pygame.mixer.music.load("ringbell.wav")
pygame.mixer.music.play()
while pygame.mixer.music.get_busy() == True:
    continue



