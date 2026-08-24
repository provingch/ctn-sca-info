export function normalizeGrade(grade: number): 1 | 2 | 3 | 4 | 5 {
  return Math.min(5, Math.max(1, Math.round(grade))) as 1 | 2 | 3 | 4 | 5;
}
