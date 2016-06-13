#!/bin/bash

# tells bash that it should exit the script if any statement returns value  > 0
set -e

CURRENT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
LOG_FILE=migration.log
rm -f $LOG_FILE
echo "Migration scripts are situated in directory '$CURRENT_DIR'" >> $LOG_FILE

#### fix mongoDB
echo "=========== fix mongoDB ======" >> $LOG_FILE
# obtain rows with admin's uids
uidsToSetPerms=$( sudo slapcat -b "$user_ldap_dn" -a "(employeeType=system/admin)" | grep "uid:" | awk -F':' '{ print $2 }' )

# translate ' admin@codenvy.com\n adm1@test\n adm2@test' into the '"admin@codenvy.com","adm1@test","adm2@test"'
uidsToSetPerms=$( echo "\"${uidsToSetPerms:1}\"" | tr -d '\n' |  sed -r 's/ /","/g' )
echo "Next admin's uids found in ldap: $uidsToSetPerms" >> $LOG_FILE

# enclose list of uids into update_mongo.js script to add permissions
sed -i "s|var ADMINS=\[\]|var ADMINS=[$uidsToSetPerms]|" ${CURRENT_DIR}/update_mongo.js &> /dev/null

mongo -u$mongo_admin_user_name -p$mongo_admin_pass --authenticationDatabase admin "${CURRENT_DIR}/update_mongo.js" &>> $LOG_FILE

echo "=========== fix url-encoded symbols in ldap ======" >> $LOG_FILE
~/codenvy-im/jre/bin/java -jar ${CURRENT_DIR}/ldap-tools-1.0-jar-with-dependencies.jar normalize-cns -url '$ldap_protocol://$ldap_host:$ldap_port' -cdn '$user_ldap_user_container_dn' -credentials '$admin_ldap_password' -principal '$java_naming_security_principal' &>> $LOG_FILE

echo "=========== fix namespaces in mognoDB ======" >> $LOG_FILE
~/codenvy-im/jre/bin/java -jar ${CURRENT_DIR}/namespace-tool-1.0-jar-with-dependencies.jar -mongo_orgservice_db_name '$mongo_orgservice_db_name' -java_naming_security_authentication '$java_naming_security_authentication' -java_naming_provider_url '$ldap_protocol://$ldap_host:$ldap_port' -mongo_workspace_collection_name 'workspaces2' -java_naming_security_credential '$admin_ldap_password' -user_ldap_user_container_dn '$user_ldap_user_container_dn' -mongo_db_url 'localhost:27017' -mongo_orgservice_user_name '$mongo_orgservice_user_name' -java_naming_security_principal '$java_naming_security_principal' -mongo_orgservice_user_pwd '$mongo_orgservice_user_pwd' &>> $LOG_FILE
