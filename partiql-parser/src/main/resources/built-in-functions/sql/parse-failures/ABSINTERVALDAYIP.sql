CREATE FUNCTION "ABS"(
    N INTERVAL ( DAY ( IP )))
    RETURNS INTERVAL
    (DAY ( IP))
    SPECIFIC ABSINTERVALDAYIP
    RETURN ABS ( N ) ;
