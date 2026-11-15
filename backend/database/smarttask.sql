-- SmartTask MySQL schema (manual JDBC, no Hibernate)
CREATE DATABASE IF NOT EXISTS smartTask
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE smartTask;

CREATE TABLE IF NOT EXISTS users (
  user_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(100) NOT NULL,
  email       VARCHAR(150) NOT NULL UNIQUE,
  password    VARCHAR(255) NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tasks (
  task_id       BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  objective_id  BIGINT NULL,
  title         VARCHAR(150) NOT NULL,
  description   VARCHAR(1000) NULL,
  priority      VARCHAR(20) NOT NULL,
  status        VARCHAR(20) NOT NULL,
  deadline      DATE NOT NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at  DATETIME NULL,
  CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
  token_hash  VARCHAR(64) PRIMARY KEY,
  user_id     BIGINT NOT NULL UNIQUE,
  expires_at  DATETIME(6) NOT NULL,
  CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS aims (
  aim_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  title       VARCHAR(200) NOT NULL,
  description VARCHAR(1000) NULL,
  deadline    DATE NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_aims_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS goals (
  goal_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
  aim_id      BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  title       VARCHAR(200) NOT NULL,
  description VARCHAR(1000) NULL,
  deadline    DATE NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_goals_aim FOREIGN KEY (aim_id) REFERENCES aims(aim_id) ON DELETE CASCADE,
  CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS objectives (
  objective_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  goal_id      BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  title        VARCHAR(200) NOT NULL,
  description  VARCHAR(1000) NULL,
  deadline     DATE NULL,
  status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_objectives_goal FOREIGN KEY (goal_id) REFERENCES goals(goal_id) ON DELETE CASCADE,
  CONSTRAINT fk_objectives_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Link existing tasks table to objectives if column was created above
-- ALTER TABLE tasks ADD COLUMN IF NOT EXISTS is not portable on older MySQL;
-- objective_id is included in CREATE TABLE above for fresh installs.

CREATE INDEX idx_tasks_user ON tasks(user_id);
CREATE INDEX idx_tasks_objective ON tasks(objective_id);
CREATE INDEX idx_aims_user ON aims(user_id);
CREATE INDEX idx_goals_aim ON goals(aim_id);
CREATE INDEX idx_objectives_goal ON objectives(goal_id);
