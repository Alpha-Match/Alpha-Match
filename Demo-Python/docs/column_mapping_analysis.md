# Column Mapping Analysis

## Purpose
This document compares the actual pkl file columns with the Pydantic domain models to guide preprocessing implementation.

---

## 1. Candidate Domain

### candidate_v1.pkl Structure
- **Total Rows**: 123,464
- **Vector Dimension**: 384d (confirmed)
- **Null Vectors**: 4,723 rows (must filter)

### Actual Columns (Title Case)
```
Position                 - object  (position display)
Moreinfo                 - object  (additional info)
Looking For              - object  (what candidate seeks)
Highlights               - object  (key achievements)
Primary Keyword          - object  (role category)
English Level            - object  (language proficiency)
Experience Years         - float64 (years of experience)
CV                       - object  (resume text)
CV_lang                  - object  (resume language)
id                       - object  (UUID)
__index_level_0__        - int64   (REMOVE: unnecessary)
skills                   - object  (numpy array of skill names)
normalized_skills        - object  (list, REMOVE: preprocessed)
embedding_input_text     - object  (REMOVE: intermediate)
skill_vector             - object  (numpy array 384d - TARGET)
embedding_sample         - object  (REMOVE: sample data)
```

### CandidateData Model Requirements
```python
candidate_id: str           # id
position_category: str      # Primary Keyword
experience_years: int       # Experience Years (convert float → int)
original_resume: str        # CV
skills: List[str]           # skills (numpy array → list)
skills_vector: List[float]  # skill_vector (numpy array → list, 384d)
```

### Column Mapping Strategy
```python
{
    'id': 'candidate_id',                # Direct rename
    'Primary Keyword': 'position_category',  # Rename
    'Experience Years': 'experience_years',   # Rename + cast to int
    'CV': 'original_resume',                 # Rename
    'skills': 'skills',                      # Keep, convert ndarray → list
    'skill_vector': 'skills_vector'          # Rename, convert ndarray → list
}
```

### Preprocessing Steps
1. **Column Rename**: Apply mapping above
2. **Type Conversion**:
   - `experience_years`: float64 → int (round if needed, handle NaN → 0 or filter)
   - `skills`: numpy.ndarray → list
   - `skills_vector`: numpy.ndarray → list
3. **Filtering**:
   - Remove rows where `skills_vector` is NaN (4,723 rows)
   - Remove rows where `skills` is empty array
   - Ensure `skills_vector` dimension = 384
4. **Drop Unnecessary Columns**:
   - `__index_level_0__`, `normalized_skills`, `embedding_input_text`, `embedding_sample`
   - `Position`, `Moreinfo`, `Looking For`, `Highlights`, `English Level`, `CV_lang`
5. **NaN Handling**:
   - Convert pandas NaN to Python None for Pydantic compatibility

### Expected Output
```python
{
    'candidate_id': '50534b61-6826-52b1-9ac5-bfd2cfa348ec',
    'position_category': 'Lead',
    'experience_years': 11,
    'original_resume': 'Landed a role of Director...',
    'skills': ['Angular', 'Ansible', 'Argo CD', ...],  # 29 items
    'skills_vector': [-0.0583191, -0.05129896, ...]    # 384 floats
}
```

---

## 2. Recruit Domain (v2)

### recruitment_v2.pkl Structure
- **Total Rows**: 93,675
- **Vector Dimension**: 384d (confirmed)
- **Additional Column**: `db_id` (not in v1)

### Actual Columns (Title Case)
```
db_id                    - int64   (NEW: database ID, REMOVE)
Position                 - object  (job title)
Long Description         - object  (job description)
Company Name             - object  (company name)
Exp Years                - object  (experience requirement - needs parsing)
Primary Keyword          - object  (job category)
English Level            - object  (language requirement)
Published                - object  (publish date)
Long Description_lang    - object  (description language)
id                       - object  (UUID)
__index_level_0__        - int64   (REMOVE: unnecessary)
skills                   - object  (numpy array of skill names)
normalized_skills        - object  (list, REMOVE: preprocessed)
embedding_input_text     - object  (REMOVE: intermediate)
skill_vector             - object  (numpy array 384d - TARGET)
embedding_sample         - object  (REMOVE: sample data)
```

### RecruitData Model Requirements (v2)
```python
id: str                          # id
position: str                    # Position
company_name: str                # Company Name
experience_years: Optional[int]  # Exp Years (parse 'no_exp', 'Ny' → int)
primary_keyword: Optional[str]   # Primary Keyword
english_level: Optional[str]     # English Level
published_at: Optional[str]      # Published
skills: List[str]                # skills (numpy array → list)
long_description: Optional[str]  # Long Description
description_lang: Optional[str]  # Long Description_lang
skills_vector: List[float]       # skill_vector (numpy array → list, 384d)
```

### v2 vs v1 Comparison
**New in v2**:
- `db_id` column (must remove)

**Same Structure**: All other columns match v1 structure

### Column Mapping Strategy
```python
{
    'Position': 'position',
    'Company Name': 'company_name',
    'Exp Years': 'experience_years',       # Parse: 'no_exp'→None, 'Ny'→int
    'Primary Keyword': 'primary_keyword',
    'English Level': 'english_level',
    'Published': 'published_at',
    'Long Description': 'long_description',
    'Long Description_lang': 'description_lang',
    'skills': 'skills',                    # Convert ndarray → list
    'skill_vector': 'skills_vector',       # Rename + convert ndarray → list
    'id': 'id'                             # Keep as-is
}
```

### Preprocessing Updates for v2
Current `_preprocess_recruit_data()` implementation (lines 137-222) should handle v2 with ONE modification:

**Required Change**:
```python
# Add db_id to unnecessary columns list
unnecessary_cols = [
    '__index_level_0__',
    'normalized_skills',
    'embedding_input_text',
    'embedding_sample',
    'db_id'  # NEW: Remove db_id column
]
```

All other preprocessing logic remains valid:
- ✅ Column mapping (Title Case → snake_case)
- ✅ Exp Years parsing ('no_exp' → None, 'Ny' → int)
- ✅ Skills/vector conversion (ndarray → list)
- ✅ Filtering empty skills/vectors
- ✅ NaN handling

---

## 3. Key Findings Summary

| Aspect | Candidate v1 | Recruitment v2 | Recruitment v1 |
|--------|--------------|----------------|----------------|
| Total Rows | 123,464 | 93,675 | 93,675 |
| Vector Dim | 384d ✅ | 384d ✅ | 384d ✅ |
| Null Vectors | 4,723 | 0 (from sample) | 6,187 |
| New Columns | - | `db_id` | - |
| Experience Field | float64 | object (needs parse) | object (needs parse) |
| Skills Format | ndarray | ndarray | ndarray |

### Critical Observations

1. **Numpy Version Compatibility**: ✅ RESOLVED
   - Upgraded numpy to 2.4.0
   - Upgraded pandas to 2.3.3
   - All pkl files now load successfully

2. **Candidate-Specific Issues**:
   - `Experience Years` is already numeric (float64) - simpler than Recruit!
   - Just need to convert to int and handle NaN
   - 4,723 rows with null vectors must be filtered

3. **Recruitment v2 Changes**:
   - Only one new column: `db_id` (must drop)
   - All other processing logic can reuse v1 implementation

4. **Common Patterns** (both domains):
   - Skills stored as numpy arrays → must convert to list
   - Vectors stored as numpy arrays → must convert to list
   - Unnecessary intermediate columns (normalized_skills, embedding_input_text, etc.)
   - NaN values must be converted to None for Pydantic

---

## 4. Implementation Checklist

### For Candidate Preprocessing (`_preprocess_candidate_data()`)
- [ ] Apply column mapping (Primary Keyword → position_category, etc.)
- [ ] Convert `Experience Years` float64 → int (handle NaN → filter or 0)
- [ ] Filter rows with null `skill_vector`
- [ ] Filter rows with empty `skills` array
- [ ] Convert numpy arrays to lists (skills, skills_vector)
- [ ] Drop unnecessary columns (9 columns to remove)
- [ ] Validate vector dimension = 384
- [ ] Convert NaN to None
- [ ] Select only required columns in correct order

### For Recruitment v2 Preprocessing (Update existing)
- [ ] Add `db_id` to unnecessary columns list (line 183)
- [ ] Test with recruitment_v2.pkl file
- [ ] Verify all 93,675 rows process correctly

---

**Generated**: 2025-12-26
**Status**: Ready for implementation
