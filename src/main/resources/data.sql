-- Seed data: locations and satellites (run after Hibernate creates schema)
-- Idempotent: safe to run multiple times

-- Locations (cities)
INSERT INTO location (name, latitude, longitude, altitude)
SELECT 'Moscow', 55.7558, 37.6173, 150.0
WHERE NOT EXISTS (SELECT 1 FROM location WHERE name = 'Moscow');

INSERT INTO location (name, latitude, longitude, altitude)
SELECT 'London', 51.5074, -0.1278, 11.0
WHERE NOT EXISTS (SELECT 1 FROM location WHERE name = 'London');

INSERT INTO location (name, latitude, longitude, altitude)
SELECT 'New York', 40.7128, -74.0060, 10.0
WHERE NOT EXISTS (SELECT 1 FROM location WHERE name = 'New York');

INSERT INTO location (name, latitude, longitude, altitude)
SELECT 'Tokyo', 35.6762, 139.6503, 40.0
WHERE NOT EXISTS (SELECT 1 FROM location WHERE name = 'Tokyo');

-- ISS (example TLE – update periodically from CelesTrak)
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

-- Optional: polygon points for Moscow (only if none exist yet)
INSERT INTO location_polygon (location_id, lat_deg, lon_deg)
SELECT l.id, 55.49, 37.32 FROM location l WHERE l.name = 'Moscow'
  AND NOT EXISTS (SELECT 1 FROM location_polygon lp WHERE lp.location_id = l.id)
LIMIT 1;
INSERT INTO location_polygon (location_id, lat_deg, lon_deg)
SELECT l.id, 56.02, 37.85 FROM location l WHERE l.name = 'Moscow'
  AND (SELECT COUNT(*) FROM location_polygon lp WHERE lp.location_id = l.id) = 1
LIMIT 1;
