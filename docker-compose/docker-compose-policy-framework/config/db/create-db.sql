#
# Create CLDS database objects (tables, etc.)
#
#
CREATE DATABASE IF NOT EXISTS `cldsdb4`;
CREATE DATABASE IF NOT EXISTS `policyadmin`;
CREATE DATABASE IF NOT EXISTS `controlloop`;
USE `cldsdb4`;
DROP USER 'clds';
CREATE USER 'clds';
GRANT ALL on cldsdb4.* to 'clds' identified by 'sidnnd83K' with GRANT OPTION;
FLUSH PRIVILEGES;

