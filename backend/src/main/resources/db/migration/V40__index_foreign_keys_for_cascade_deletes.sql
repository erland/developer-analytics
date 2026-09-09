DO $$
DECLARE
    fk RECORD;
    index_name TEXT;
BEGIN
    FOR fk IN
        SELECT
            c.conrelid,
            c.conname,
            n.nspname,
            cls.relname,
            a.attname,
            c.conkey[1] AS attnum
        FROM pg_constraint c
        JOIN pg_class cls ON cls.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = cls.relnamespace
        JOIN pg_attribute a
          ON a.attrelid = c.conrelid
         AND a.attnum = c.conkey[1]
        WHERE c.contype = 'f'
          AND array_length(c.conkey, 1) = 1
          AND n.nspname = current_schema()
          AND NOT EXISTS (
              SELECT 1
              FROM pg_index i
              WHERE i.indrelid = c.conrelid
                AND i.indisvalid
                AND i.indisready
                AND i.indkey[0] = c.conkey[1]
          )
    LOOP
        index_name := substr('idx_fk_' || fk.relname || '_' || fk.attname, 1, 54)
                      || '_' || substr(md5(fk.conname), 1, 8);
        EXECUTE format(
            'CREATE INDEX %I ON %I.%I (%I)',
            index_name,
            fk.nspname,
            fk.relname,
            fk.attname
        );
    END LOOP;
END $$;
