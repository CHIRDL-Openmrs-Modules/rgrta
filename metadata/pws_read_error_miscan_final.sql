INSERT INTO `concept` (retired, description, datatype_id, class_id,
 is_set, creator, date_created)  VALUES ( '0', 'PWS READ ERROR',
 '2', (select concept_class_id from concept_class where name like 'CHICA'), '1', '1', now());

INSERT INTO `concept` (retired, description, datatype_id, class_id, 
 is_set, creator, date_created)  VALUES ( '0', 'MISSCAN',
 '2', (select concept_class_id from concept_class where name like 'CHICA'), '1', '1', now());	 
 
INSERT INTO `concept_name`
(concept_id, name, locale, creator, date_created, voided)
VALUES ((select concept_id from concept where description like 'PWS READ ERROR'),
'PWS READ ERROR', 'en', '1', now(),'0');

INSERT INTO `concept_name`
(concept_id, name, locale, creator, date_created, voided)
VALUES ((select concept_id from concept where description like 'MISSCAN'),
'MISSCAN', 'en', '1', now(),'0');

INSERT INTO `concept_answer`
(concept_id, answer_concept, answer_drug, creator, date_created)
 VALUES ((select concept_id from concept_name where name like 'PWS READ ERROR'),
 (select concept_id from concept_name where name like 'MISSCAN'),
 null, '1', now());
 
 INSERT INTO `concept_word`
VALUES ((select concept_id from concept
where description like 'PWS READ ERROR'), 'PWS', '', 'en',
(select concept_name_id from concept_name where name like 'PWS READ ERROR'));

INSERT INTO `concept_word`
VALUES ((select concept_id from concept
where description like 'PWS READ ERROR'), 'ERROR', '', 'en',
(select concept_name_id from concept_name where name like 'PWS READ ERROR'));

INSERT INTO `concept_word`
VALUES ((select concept_id from concept
where description like 'PWS READ ERROR'), 'READ', '', 'en',
(select concept_name_id from concept_name where name like 'PWS READ ERROR'));

INSERT INTO `concept_word`
VALUES ((select concept_id from concept
where description like 'MISSCAN'), 'MISSCAN', '', 'en',
(select concept_name_id from concept_name where name like 'MISSCAN'));