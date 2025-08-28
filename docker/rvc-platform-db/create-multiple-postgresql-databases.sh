#!/bin/bash

# Credits: https://github.com/mrts/docker-postgresql-multiple-databases/blob/master/create-multiple-postgresql-databases.sh

set -e
set -u

function create_database() {
	local database=$1
	echo "Creating user and database '$database'"

	username_env_var="${database}_USER"
	username_env_value=$(printenv "$username_env_var")
	password_env_var="${database}_PASSWORD"
	password_env_value=$(printenv "$password_env_var")

  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
	    CREATE USER $username_env_value WITH ENCRYPTED PASSWORD '$password_env_value';

	    CREATE DATABASE $database;

	    GRANT ALL PRIVILEGES ON DATABASE $database TO $username_env_value;
      \c $database;
      GRANT USAGE, CREATE ON SCHEMA public TO $username_env_value;
      GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON ALL TABLES IN SCHEMA public TO $username_env_value;
      GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO $username_env_value;
EOSQL
}

if [ -n "$POSTGRES_DATABASES" ]; then
	echo "Creating multiple databases: $POSTGRES_DATABASES"
	for db in $(echo $POSTGRES_DATABASES | tr ',' ' '); do
		create_database $db
	done
	echo "Requested databases have been created"
fi
