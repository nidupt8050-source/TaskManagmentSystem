# SmartTask — Task Management System

Java / Spring Boot web app for CSA201. Users register and log in, then create, prioritize, search, sort, and track personal tasks with deadline reminders. Data is stored in MySQL; the business layer also uses the ADS data structures from the proposal (ArrayList, HashMap, PriorityQueue, Stack, LinkedList, TaskBST, plus sorting/searching algorithms).

## Stack

- Frontend: Thymeleaf + HTML/CSS
- Backend: Spring Boot (Java)
- Database: MySQL 8
- Auth: HTTP session + BCrypt password hashing

## Setup

1. Start the local MySQL service (`MySQL80`) from Services or an elevated terminal:
   ```bat
   net start MySQL80
   ```
2. Copy `.env.example` to `.env` and set your credentials:
   ```env
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=smartTask
   DB_USERNAME=root
   DB_PASSWORD=your_mysql_password
   SERVER_PORT=8080
   ```
3. Run the app with the Maven wrapper:
   ```bat
   .\mvnw.cmd spring-boot:run
   ```
4. Open [http://localhost:8080](http://localhost:8080)

Tables (`users`, `tasks`) are created automatically on startup.

## Main features

- Register / login / logout
- Task CRUD with priority, status, and deadlines
- Dashboard progress, reminders, and earliest-deadline queue
- Search (linear + BST) and sort (quick / merge / insertion)
- Status history (LinkedList) and undo last status change (Stack)
