from django.urls import path

from . import views


urlpatterns = [
    path('', views.health_check),
    path('favicon.<str:file_extension>', views.favicon),

    path('scenes', views.search),
    path('scenes/<str:scene_id>', views.get_scene),
    path('tiles/<str:scene_id>/<int:z>/<int:x>/<int:y>.png', views.get_tile),
]
