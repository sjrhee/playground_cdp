crdp-file-converter sample_data/employee_export.csv -e -c10 --host 192.168.0.234 -p3 --policy dev-users-policy --user dev-user01
sleep 1
mv e01_employee_export.csv sample_data/
