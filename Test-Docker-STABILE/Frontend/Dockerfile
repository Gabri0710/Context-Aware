# Usa un'immagine leggera di un server web basata su Nginx
FROM nginx:alpine

# Copia i file del frontend nella directory del server web di Nginx
COPY . /usr/share/nginx/html


EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]