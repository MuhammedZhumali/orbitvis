-- Optional: run this manually if you need to (re)seed before the app starts.
-- Normally the app uses data.sql on startup (spring.sql.init.mode=always).
-- Tables must already exist (created by Hibernate on first app run).

-- Example: insert one satellite (ISS) and a few locations.
-- Adjust UUID and TLE as needed.

INSERT INTO location (name, latitude, longitude, altitude)
SELECT 'Moscow', 55.7558, 37.6173, 150.0
WHERE NOT EXISTS (SELECT 1 FROM location WHERE name = 'Moscow');

INSERT INTO location (name, latitude, longitude, altitude)
SELECT 'London', 51.5074, -0.1278, 11.0
WHERE NOT EXISTS (SELECT 1 FROM location WHERE name = 'London');

INSERT INTO satellite (satellite_id, line1, line2, max_nadir_off_angle, min_sun_angle, max_roll_angle)
VALUES (
  'a1b2c3d4-e5f6-4789-a012-345678901234'::uuid,
  '1 25544U 98067A   26065.51782528  .00016717  00000+0  10270-3 0  9008',
  '2 25544  51.6400 208.9163 0006317  27.4038 332.7514 15.49798082445652',
  5.0,
  30.0,
  45.0
)
ON CONFLICT (satellite_id) DO NOTHING;
