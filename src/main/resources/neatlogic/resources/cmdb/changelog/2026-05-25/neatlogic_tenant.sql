UPDATE mq_topic
SET handler = 'artemis-topic'
WHERE name IN ('cmdb/cientity/delete', 'cmdb/cientity/insert', 'cmdb/cientity/recover', 'cmdb/cientity/update')
  AND handler = 'artemis';

UPDATE mq_subscribe
SET handler = 'artemis-topic'
WHERE topic_name IN ('cmdb/cientity/delete', 'cmdb/cientity/insert', 'cmdb/cientity/recover', 'cmdb/cientity/update')
  AND handler = 'artemis';
