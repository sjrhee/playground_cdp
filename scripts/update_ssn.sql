SET @seq = 1234567890122;
UPDATE employee
SET ssn_no = (@seq := @seq + 1)
ORDER BY emp_no;

-- ./run_sql.sh < update_ssn.sql
