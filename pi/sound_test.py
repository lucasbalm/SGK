import pygame
import time
from time import sleep

import winsound

pygame.mixer.init()
def playsound(sound, soundtime):
    pygame.mixer.music.load(sound)
    pygame.mixer.music.set_endevent(pygame.constants.USEREVENT)
    pygame.mixer.music.play()
    sleep(soundtime)


playsound("takepic_again.wav",9)
playsound("calling_owner.wav",5)
playsound("norecog.wav",9)
playsound("acess_denied.wav",4)
playsound("acess_granted.wav",4)
