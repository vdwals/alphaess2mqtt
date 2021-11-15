CREATE TABLE tasks (
  id  int(11) DEFAULT NULL auto_increment PRIMARY KEY,
  description VARCHAR(128),
  user_id int(11),
  created_at DATETIME,
  updated_at DATETIME
)ENGINE=InnoDB;
