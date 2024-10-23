alter table note
add column url varchar(16) not null ,
    add column expiration_type smallint,
    add column is_available boolean default true;