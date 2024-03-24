
 SELECT 
    T3.StudentID,
    SUM(CASE
        WHEN T3.NumScans IS NULL THEN 1
        ELSE 0
    END) AS absences,
    SUM(CASE
        WHEN ABS(TIME_TO_SEC(T3.duration)) < 3600 THEN 1
        ELSE 0
    END) AS onewayScan
FROM
    (SELECT 
        t1.*,
            t2.FirstScan,
            t2.LastScan,
            t2.NumScans,
            TIMEDIFF(t2.LastScan, t2.FirstScan) AS duration
    FROM
        (SELECT 
        s.ID AS StudentID,
            s.Name AS StudentName,
            s.Color AS ColorGroup,
            d.CourseDay,
            d.session
    FROM
        student s
    CROSS JOIN (
                SELECT '2024-02-12' AS CourseDay, 'Morning' AS session UNION ALL SELECT '2024-02-12', 'Afternoon' UNION ALL SELECT '2024-02-13', 'Morning' UNION ALL SELECT '2024-02-13', 'Afternoon' UNION ALL SELECT '2024-02-14', 'Morning' UNION ALL SELECT '2024-02-14', 'Afternoon' UNION ALL SELECT '2024-02-15', 'Morning' UNION ALL SELECT '2024-02-15', 'Afternoon' UNION ALL SELECT '2024-02-16', 'Morning' UNION ALL SELECT '2024-02-16', 'Afternoon' UNION ALL SELECT '2024-02-17', 'Morning' UNION ALL SELECT '2024-02-19', 'Morning' UNION ALL SELECT '2024-02-19', 'Afternoon' UNION ALL SELECT '2024-02-20', 'Morning' UNION ALL SELECT '2024-02-20', 'Afternoon' UNION ALL SELECT '2024-02-21', 'Morning' UNION ALL SELECT '2024-02-21', 'Afternoon' UNION ALL SELECT '2024-02-22', 'Morning' UNION ALL SELECT '2024-02-22', 'Afternoon'         
        ) d) AS T1
    LEFT JOIN (SELECT 
        SCANS.*
    FROM
        (SELECT 
        a_SESSION.StudentID,
            DATE(a_SESSION.ScanDate) AS dayScan,
            a_SESSION.Session,
            MIN(a_SESSION.ScanDate) AS FirstScan,
            MAX(a_SESSION.ScanDate) AS LastScan,
            COUNT(*) AS NumScans
    FROM
        (SELECT 
        StudentID,
            Date AS ScanDate,
            CASE
                WHEN HOUR(Date) < 13 THEN 'Morning'
                WHEN HOUR(Date) >= 13 THEN 'Afternoon'
            END AS Session
    FROM
        attendance) AS a_SESSION
    GROUP BY a_SESSION.StudentID , DATE(a_SESSION.ScanDate) , a_SESSION.Session
    ORDER BY a_SESSION.StudentID , DATE(a_SESSION.ScanDate) , a_SESSION.Session) AS SCANS) AS T2 ON T1.StudentID = T2.StudentID
        AND T1.CourseDay = T2.dayScan
        AND T1.Session = T2.Session) AS T3
GROUP BY T3.StudentID
HAVING absences >= ?