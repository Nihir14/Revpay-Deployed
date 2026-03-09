# Stage 1: Build the Angular application
FROM node:20 AS build
WORKDIR /app

# Install dependencies first (takes advantage of Docker caching)
COPY package*.json ./
RUN npm install

# Copy the rest of the code and build the project
COPY . .
RUN npm run build -- --configuration production

# Stage 2: Serve the application with Nginx
FROM nginx:alpine

# Copy the custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy the compiled Angular build from Stage 1 to the Nginx HTML folder
COPY --from=build /app/dist/revpay-frontend/browser /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
