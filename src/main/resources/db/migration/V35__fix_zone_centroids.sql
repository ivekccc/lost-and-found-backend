-- ST_Centroid ne garantuje tacku UNUTAR geometrije: za izduzene ili konkavne
-- opstine (npr. Palilula, koja se pruza preko Dunava) centroid moze pasti u
-- susednu opstinu, pa bi marker zone na mapi sedeo na pogresnom mestu.
-- ST_PointOnSurface je uvek unutar poligona.
UPDATE zones
SET centroid_latitude = ST_Y(ST_PointOnSurface(boundary)),
    centroid_longitude = ST_X(ST_PointOnSurface(boundary));
