from django.urls import path

from . import views


urlpatterns = [
    path('', views.health_check),
    path('favicon.<str:file_extension>', views.favicon),

    path('scenes/<str:scene_id>', views.get_scene),
]
