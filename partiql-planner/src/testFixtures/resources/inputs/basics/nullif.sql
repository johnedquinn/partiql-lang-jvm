--#[nullif-00]
-- Currently, no constant-folding. If there was, return type could be int32.
-- type: (int32)
NULLIF(1, 2);

--#[nullif-01]
-- Currently, no constant-folding. If there was, return type could be null.
-- type: (int32)
NULLIF(1, 1);

--#[nullif-02]
-- type: (int32)
NULLIF(t_item.t_int32, t_item.t_int32);

--#[nullif-03]
-- type: (int32)
NULLIF(t_item.t_int32, t_item.t_int64);

--#[nullif-04]
-- type: (int64)
NULLIF(t_item.t_int64, t_item.t_int32);

--#[nullif-05]
-- type: (int32)
NULLIF(t_item.t_int32, NULL);

--#[nullif-06]
-- type: (any)
NULLIF(NULL, t_item.t_int32);

--#[nullif-07]
-- type: (int32)
NULLIF(t_item.t_int32, MISSING);

--#[nullif-08]
-- type: (any)
NULLIF(MISSING, t_item.t_int32);

--#[nullif-09]
-- type: (int32)
NULLIF(t_item.t_int32, t_item.t_int32);

--#[nullif-11]
-- type: (int32)
NULLIF(t_item.t_int32, t_item.t_int64);

--#[nullif-12]
-- type: (int64)
NULLIF(t_item.t_int64, t_item.t_int32);

--#[nullif-13]
-- type: (int32)
NULLIF(t_item.t_int32, t_item.t_string);

--#[nullif-14]
-- type: (string)
NULLIF(t_item.t_string, t_item.t_int32);

--#[nullif-15]
-- type: (int32)
NULLIF(t_item.t_int32, t_item.t_num_exact);

--#[nullif-16]
-- type: (int16 | int32 | int64 | int | decimal)
NULLIF(t_item.t_num_exact, t_item.t_int32);

--#[nullif-17]
-- type: (int32)
NULLIF(t_item.t_int32, t_item.t_any);

--#[nullif-18]
-- type: (any)
NULLIF(t_item.t_any, t_item.t_int32);
