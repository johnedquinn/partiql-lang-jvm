CREATE FUNCTION "ABS"(
    N INTERVAL ( HOUR ( IP ) TO SECOND ( IS )))
    RETURNS INTERVAL
    (HOUR ( IP) TO SECOND (IS))
    SPECIFIC ABSINTERVALHOURIP_SECONDIS
    RETURN ABS ( N ) ;