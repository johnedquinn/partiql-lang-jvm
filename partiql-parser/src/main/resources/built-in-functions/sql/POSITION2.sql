CREATE FUNCTION "POSITION" (
    S1 CHARACTER VARYING ( CML ),
    S2 CHARACTER ( CML ) )
    RETURNS NUMERIC ( P1, 0 )
    SPECIFIC POSITION2
    RETURN POSITION ( S1 IN S2 ) ;