-- SET TIMEZONE TO UTC
SET time_zone = '+00:00';


-- TABLES
CREATE TABLE if not exists user (
    user_id BINARY(16) PRIMARY KEY,
    username VARCHAR(32) NOT NULL,
    coins INT UNSIGNED DEFAULT 5000,
    level SMALLINT UNSIGNED DEFAULT 1,
    country ENUM('Turkey', 'the United States', 'the United Kingdom', 'France', 'Germany'),
    created_at DATETIME,
    CONSTRAINT username_length CHECK (LENGTH(username) >= 1 AND LENGTH(username) <= 32),
    CONSTRAINT username_ascii CHECK (username REGEXP '^[[:ascii:]]+$')
);

CREATE TABLE if not exists tournament_group (
    group_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    date_formed DATETIME NOT NULL
);

CREATE TABLE if not exists user_in_tournament (
    group_id INT UNSIGNED,
    user_id BINARY(16),
    score SMALLINT UNSIGNED DEFAULT 0,
    reward SMALLINT UNSIGNED DEFAULT 0,
    is_reward_claimed BOOLEAN DEFAULT true,
    PRIMARY KEY (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES tournament_group(group_id),
    FOREIGN KEY (user_id) REFERENCES user(user_id),
    CHECK ((reward = 0 AND is_reward_claimed = true) OR reward != 0) -- Ensure that if reward is 0, then is_reward_claimed must be true 
);


-- TRIGGERS
-- Randomly assign a country to a user before the insertion if the country is not specified
DELIMITER //
CREATE TRIGGER set_random_country 
BEFORE INSERT ON user 
FOR EACH ROW 
BEGIN  
    DECLARE country_index INT;     
    IF NEW.country IS NULL THEN         
        SET country_index = FLOOR(RAND() * 5);       

        CASE country_index             
            WHEN 0 THEN SET NEW.country = 'Turkey';             
            WHEN 1 THEN SET NEW.country = 'the United States';             
            WHEN 2 THEN SET NEW.country = 'the United Kingdom';             
            WHEN 3 THEN SET NEW.country = 'France';             
            WHEN 4 THEN SET NEW.country = 'Germany';         
        END CASE;     
    END IF; 
END //
DELIMITER ;

-- After users pass a level, increment their scores in their group if they are a part of a tournament group that is active  
DELIMITER //
CREATE TRIGGER update_tournament_score
AFTER UPDATE ON user
FOR EACH ROW
BEGIN
    -- Check if the coins field was incremented
    IF NEW.level > OLD.level THEN
        -- Update the score field in user_in_tournament if the date_formed is within the specified time range and the same day
        UPDATE user_in_tournament uit
        JOIN tournament_group tg ON uit.group_id = tg.group_id
        SET uit.score = uit.score + 1
        WHERE uit.user_id = NEW.user_id
        AND DATE(tg.date_formed) = DATE(NOW())
        AND TIME(tg.date_formed) BETWEEN '00:00:00' AND '20:00:00';
    END IF;
END;
//
DELIMITER ;

-- When the is_reward_claimed field is set to true, update the user's coins by the reward amount
DELIMITER //
CREATE TRIGGER update_coins_on_reward_claim AFTER UPDATE ON user_in_tournament
FOR EACH ROW
BEGIN
    IF OLD.is_reward_claimed = false AND NEW.is_reward_claimed = true THEN
        UPDATE user
        SET coins = coins + OLD.reward
        WHERE user_id = NEW.user_id;
    END IF;
END;
//
DELIMITER ;

-- No INSERT or UPDATE operation can be performed on the tournament_group table between 20.00 and 00.00 UTC 
DELIMITER //
CREATE TRIGGER tg_tournament_group_insert
BEFORE INSERT ON tournament_group
FOR EACH ROW
BEGIN
    IF HOUR(UTC_TIMESTAMP()) BETWEEN 20 AND 23 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Inserts are not allowed between 20:00 and 00:00 UTC';
    END IF;
END //

CREATE TRIGGER tg_tournament_group_update
BEFORE UPDATE ON tournament_group
FOR EACH ROW
BEGIN
    IF HOUR(UTC_TIMESTAMP()) BETWEEN 20 AND 23 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Updates are not allowed between 20:00 and 00:00 UTC';
    END IF;
END //
DELIMITER ;

-- No INSERT operation can be performed on the user_in_tournament table between 20.00 and 00.00 UTC
DELIMITER //
CREATE TRIGGER tg_user_in_tournament_insert
BEFORE INSERT ON user_in_tournament
FOR EACH ROW
BEGIN
    IF HOUR(UTC_TIMESTAMP()) BETWEEN 20 AND 23 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Inserts are not allowed between 20:00 and 00:00 UTC';
    END IF;
END //

-- The 'score' field of the user_in_tournament table cannot be UPDATED between 20.00 and 00.00 UTC
CREATE TRIGGER tg_user_in_tournament_update_score
BEFORE UPDATE ON user_in_tournament
FOR EACH ROW
BEGIN
    IF HOUR(UTC_TIMESTAMP()) BETWEEN 20 AND 23 THEN
        IF NEW.score <> OLD.score THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Updates to the score field are not allowed between 22:00 and 00:00 UTC';
        END IF;
    END IF;
END //
DELIMITER ;

-- Datetime Setters
DELIMITER //
CREATE TRIGGER set_created_at
BEFORE INSERT ON user
FOR EACH ROW
BEGIN
    IF NEW.created_at IS NULL THEN
        SET NEW.created_at = UTC_TIMESTAMP();
    END IF;
END;

CREATE TRIGGER set_date_formed
BEFORE INSERT ON tournament_group
FOR EACH ROW
BEGIN
    IF NEW.date_formed IS NULL THEN
        SET NEW.date_formed = UTC_TIMESTAMP();
    END IF;
END;
//


-- Populate Database
-- Create 5 users and place them into a group
INSERT INTO user (user_id, username, coins, level, country, created_at) VALUES
    (UNHEX(REPLACE('7c55f0b9-0353-4c55-b12d-2f3005155cc6', '-', '')), 'Winston Churchil', 5475, 20, 'the United Kingdom', '2024-06-06 01:00:00'),
    (UNHEX(REPLACE('952730dc-01e7-4de1-bd9f-74872aebf572', '-', '')), 'Otto von Bismark', 5475, 20, 'Germany', '2024-06-06 01:00:00'),
    (UNHEX(REPLACE('e5717bff-4094-4951-9772-b66fd60bbd2c', '-', '')), 'George Washington', 5475, 20, 'the United States', '2024-06-06 01:00:00'),
    (UNHEX(REPLACE('a75b1445-b0bd-4e07-ab39-baca7469515f', '-', '')), 'Napoleon Bonaparte', 5475, 20, 'France', '2024-06-06 01:00:00'),
    (UNHEX(REPLACE('cbf3e573-5854-4a4b-8e74-e3546b18ffcb', '-', '')), 'Emirkan', 5475, 20, 'Turkey', '2024-06-06 01:00:00');

INSERT INTO tournament_group (group_id, date_formed) VALUES (1, '2024-06-06 06:00:00');

INSERT INTO user_in_tournament (group_id, user_id, score, reward, is_reward_claimed) VALUES
    (1, UNHEX(REPLACE('7c55f0b9-0353-4c55-b12d-2f3005155cc6', '-', '')), 1, 0, true),   
    (1, UNHEX(REPLACE('952730dc-01e7-4de1-bd9f-74872aebf572', '-', '')), 2, 0, true),
    (1, UNHEX(REPLACE('e5717bff-4094-4951-9772-b66fd60bbd2c', '-', '')), 3, 0, true),
    (1, UNHEX(REPLACE('a75b1445-b0bd-4e07-ab39-baca7469515f', '-', '')), 4, 5000, true),
    (1, UNHEX(REPLACE('cbf3e573-5854-4a4b-8e74-e3546b18ffcb', '-', '')), 5, 10000, false);

-- Auth tokens for the users (SECRET_KEY=5pAq6zRyX8bC3dV2wS7gN1mK9jF0hL4tUoP6iBvE3nG8xZaQrY7cW2fA):
    -- 7c55f0b9-0353-4c55-b12d-2f3005155cc6 -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcIjdjNTVmMGI5LTAzNTMtNGM1NS1iMTJkLTJmMzAwNTE1NWNjNlwiLFwidXNlcm5hbWVcIjpcIldpbnN0b24gQ2h1cmNoaWxcIixcImNvdW50cnlcIjpcInRoZSBVbml0ZWQgS2luZ2RvbVwifSJ9.GWudgLwqT91lOzB9uZ2od5qbXF8ffbr1NWIerntzrTY
    -- 952730dc-01e7-4de1-bd9f-74872aebf572 -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcIjk1MjczMGRjLTAxZTctNGRlMS1iZDlmLTc0ODcyYWViZjU3MlwiLFwidXNlcm5hbWVcIjpcIk90dG8gdm9uIEJpc21hcmtcIixcImNvdW50cnlcIjpcIkdlcm1hbnlcIn0ifQ.aQ3PUmGwIr_ilnbhSXeVKl-bkrQkvbQsbptq1FjMP7c
    -- e5717bff-4094-4951-9772-b66fd60bbd2c -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcImU1NzE3YmZmLTQwOTQtNDk1MS05NzcyLWI2NmZkNjBiYmQyY1wiLFwidXNlcm5hbWVcIjpcIkdlb3JnZSBXYXNoaW5ndG9uXCIsXCJjb3VudHJ5XCI6XCJ0aGUgVW5pdGVkIFN0YXRlc1wifSJ9.SCItCM-X4zDaKKJpFltRqArxN3QWTwtFytrvdOIt98U
    -- a75b1445-b0bd-4e07-ab39-baca7469515f -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcImE3NWIxNDQ1LWIwYmQtNGUwNy1hYjM5LWJhY2E3NDY5NTE1ZlwiLFwidXNlcm5hbWVcIjpcIk5hcG9sZW9uIEJvbmFwYXJ0ZVwiLFwiY291bnRyeVwiOlwiRnJhbmNlXCJ9In0.tYFZD-oc5-yz_DZUKLBtLf4IRQ_hqPxe1NfaCblYyuY
    -- cbf3e573-5854-4a4b-8e74-e3546b18ffcb -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcImNiZjNlNTczLTU4NTQtNGE0Yi04ZTc0LWUzNTQ2YjE4ZmZjYlwiLFwidXNlcm5hbWVcIjpcIkVtaXJrYW5cIixcImNvdW50cnlcIjpcIlR1cmtleVwifSJ9.hZ_Ny44vmDoTrQL11WcHyqFFyjpjL0B3BOjerVxs3I0

-- Create 10 users eligible to enter a tournament
INSERT INTO user (user_id, username, coins, level, country, created_at) VALUES
    (UNHEX(REPLACE('d83b479a-9a26-4b48-8404-ef77127c1199', '-', '')), 'Winston Churchil PRO', 5475, 20, 'the United Kingdom', UTC_TIMESTAMP()),
    (UNHEX(REPLACE('e3836464-1087-4ba4-a57e-8100d4ef624a', '-', '')), 'Otto von Bismark PRO', 5475, 20, 'Germany', UTC_TIMESTAMP()),
    (UNHEX(REPLACE('006b9d02-9723-4185-9d0b-ec07ae09bc39', '-', '')), 'George Washington PRO', 5475, 20, 'the United States', UTC_TIMESTAMP()),
    (UNHEX(REPLACE('0edec5bb-21bb-4cc3-af1f-1a86b77dbd07', '-', '')), 'Napoleon Bonaparte PRO', 5475, 20, 'France', UTC_TIMESTAMP()),
    (UNHEX(REPLACE('dca6f0f5-99f9-4a67-9d29-6a7d1a4a223b', '-', '')), 'Emirkan PRO', 5475, 20, 'Turkey', UTC_TIMESTAMP()),
    (UNHEX(REPLACE('cd6ce76e-a2b4-4141-9e0a-f3a8d9b2682e', '-', '')), 'Winston Churchil PRO MAX', 5475, 20, 'the United Kingdom', UTC_TIMESTAMP()),
    (UNHEX(REPLACE('624fdfda-27eb-49de-bbf2-40281447eb7a', '-', '')), 'Otto von Bismark PRO MAX', 5475, 20, 'Germany', UTC_TIMESTAMP()),
    (UNHEX(REPLACE('70715f5e-b79d-46ee-9bad-51b11e9f1fdf', '-', '')), 'George Washington PRO MAX', 5475, 20, 'the United States', UTC_TIMESTAMP()),
    (UNHEX(REPLACE('0629105b-6354-4312-a3c7-db1ace5d7ca3', '-', '')), 'Napoleon Bonaparte PRO MAX', 5475, 20, 'France', UTC_TIMESTAMP()),
    (UNHEX(REPLACE('8129e5d1-da29-421f-8321-de28a4c1b859', '-', '')), 'Emirkan PRO MAX', 5475, 20, 'Turkey', UTC_TIMESTAMP());

-- Auth tokens for the users (SECRET_KEY=5pAq6zRyX8bC3dV2wS7gN1mK9jF0hL4tUoP6iBvE3nG8xZaQrY7cW2fA):
    -- d83b479a-9a26-4b48-8404-ef77127c1199 -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcImQ4M2I0NzlhLTlhMjYtNGI0OC04NDA0LWVmNzcxMjdjMTE5OVwiLFwidXNlcm5hbWVcIjpcIldpbnN0b24gQ2h1cmNoaWwgUFJPXCIsXCJjb3VudHJ5XCI6XCJ0aGUgVW5pdGVkIEtpbmdkb21cIn0ifQ.Y0jFiezs1Xmx8LhKgFO7ueMmcCwd2k0oyfbEz18dVus
    -- e3836464-1087-4ba4-a57e-8100d4ef624a -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcImUzODM2NDY0LTEwODctNGJhNC1hNTdlLTgxMDBkNGVmNjI0YVwiLFwidXNlcm5hbWVcIjpcIk90dG8gdm9uIEJpc21hcmsgUFJPXCIsXCJjb3VudHJ5XCI6XCJHZXJtYW55XCJ9In0.dfM_y15jZoHF_gI17pR6vqmDcOTTgBIb_GerhoMlqXY
    -- 006b9d02-9723-4185-9d0b-ec07ae09bc39 -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcIjAwNmI5ZDAyLTk3MjMtNDE4NS05ZDBiLWVjMDdhZTA5YmMzOVwiLFwidXNlcm5hbWVcIjpcIkdlb3JnZSBXYXNoaW5ndG9uIFBST1wiLFwiY291bnRyeVwiOlwidGhlIFVuaXRlZCBTdGF0ZXNcIn0ifQ.F1S7C-TNM9qpfkqVZckBFqwma5t2h6kiObbSvDT45uY 
    -- 0edec5bb-21bb-4cc3-af1f-1a86b77dbd07 -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcIjBlZGVjNWJiLTIxYmItNGNjMy1hZjFmLTFhODZiNzdkYmQwN1wiLFwidXNlcm5hbWVcIjpcIk5hcG9sZW9uIEJvbmFwYXJ0ZSBQUk9cIixcImNvdW50cnlcIjpcIkZyYW5jZVwifSJ9.wDs7_HVbi_VlHnM3dYxfXzfWEBnBLTjYkX-tkW51sls
    -- dca6f0f5-99f9-4a67-9d29-6a7d1a4a223b -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcImRjYTZmMGY1LTk5ZjktNGE2Ny05ZDI5LTZhN2QxYTRhMjIzYlwiLFwidXNlcm5hbWVcIjpcIkVtaXJrYW4gUFJPXCIsXCJjb3VudHJ5XCI6XCJUdXJrZXlcIn0ifQ.JClcsY-Hj9yV7npygvD2qFSGr1NTifRliBar1eeDams
    -- cd6ce76e-a2b4-4141-9e0a-f3a8d9b2682e -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcImNkNmNlNzZlLWEyYjQtNDE0MS05ZTBhLWYzYThkOWIyNjgyZVwiLFwidXNlcm5hbWVcIjpcIldpbnN0b24gQ2h1cmNoaWwgUFJPIE1BWFwiLFwiY291bnRyeVwiOlwidGhlIFVuaXRlZCBLaW5nZG9tXCJ9In0.loBfjLA2UwC5u1tBB3MnMQObkUurpgfoSeggDp9yvAk
    -- 624fdfda-27eb-49de-bbf2-40281447eb7a -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcIjYyNGZkZmRhLTI3ZWItNDlkZS1iYmYyLTQwMjgxNDQ3ZWI3YVwiLFwidXNlcm5hbWVcIjpcIk90dG8gdm9uIEJpc21hcmsgUFJPIE1BWFwiLFwiY291bnRyeVwiOlwiR2VybWFueVwifSJ9._i3yI9hPPHlxmnbP1isIgdNugWcpDLe9OY-xcqX7mmQ
    -- 70715f5e-b79d-46ee-9bad-51b11e9f1fdf -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcIjcwNzE1ZjVlLWI3OWQtNDZlZS05YmFkLTUxYjExZTlmMWZkZlwiLFwidXNlcm5hbWVcIjpcIkdlb3JnZSBXYXNoaW5ndG9uIFBSTyBNQVhcIixcImNvdW50cnlcIjpcInRoZSBVbml0ZWQgU3RhdGVzXCJ9In0.w79_g5vwY76euli8_mCQhNWq-tib2BoTvl3CBUKFmlI
    -- 0629105b-6354-4312-a3c7-db1ace5d7ca3 -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcIjA2MjkxMDViLTYzNTQtNDMxMi1hM2M3LWRiMWFjZTVkN2NhM1wiLFwidXNlcm5hbWVcIjpcIk5hcG9sZW9uIEJvbmFwYXJ0ZSBQUk8gTUFYXCIsXCJjb3VudHJ5XCI6XCJGcmFuY2VcIn0ifQ.D_RstwTMnbbVdQ0BgLDdJMC7Sx5-3AKO7vX7nPNLYOI
    -- 8129e5d1-da29-421f-8321-de28a4c1b859 -> eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1c2VySURcIjpcIjgxMjllNWQxLWRhMjktNDIxZi04MzIxLWRlMjhhNGMxYjg1OVwiLFwidXNlcm5hbWVcIjpcIkVtaXJrYW4gUFJPIE1BWFwiLFwiY291bnRyeVwiOlwiVHVya2V5XCJ9In0.DPTwX9bws-hKRnH4kDX4ruMSfpPf_TJtjl4hqgQnR74

