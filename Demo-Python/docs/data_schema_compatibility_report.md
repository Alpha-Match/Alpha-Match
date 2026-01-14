# Data Schema Compatibility Report

**Date:** 2025-12-22
**Purpose:** Verify compatibility between existing data files and v2 Python/Java code expectations

---

## Executive Summary

**Status: COMPATIBLE**

Both data files (`recruitment_v1.pkl` and `skill_embeddings.json`) are **fully compatible** with the v2 schema expectations. All required fields are present with correct naming conventions and vector dimensions match the expected 384d.

---

## 1. Recruitment Data (`recruitment_v1.pkl`)

### File Structure
- **Format:** Pickle file containing a dictionary with keys: `['data_frame', 'skill_embeddings', 'metadata']`
- **Main Data:** `pkl_data['data_frame']` is a pandas DataFrame
- **Total Rows:** 93,675 recruitment records
- **Vector Coverage:** 87,488 rows (93%) have non-null skill vectors

### Schema Analysis

#### Actual Columns (15 total)
```
Position
Long Description
Company Name
Exp Years
Primary Keyword
English Level
Published
Long Description_lang
id
__index_level_0__
skills
normalized_skills
embedding_input_text
embedding_sample
skill_vector
```

#### v2 Field Mapping

| v2 Expected Field | Actual Column Name | Status | Notes |
|-------------------|-------------------|--------|-------|
| `id` | `id` | OK | UUID field exists |
| `position` | `Position` | OK | Job title |
| `company_name` | `Company Name` | OK | Company name |
| `experience_years` | `Exp Years` | OK | Experience requirement |
| `primary_keyword` | `Primary Keyword` | OK | Main keyword |
| `english_level` | `English Level` | OK | English proficiency |
| `published_at` | `Published` | OK | Publication timestamp |
| `skills` | `skills` or `normalized_skills` | OK | Both available, use `skills` |
| `long_description` | `Long Description` | OK | Full job description |
| `description_lang` | `Long Description_lang` | OK | Language code |
| `skills_vector` | `skill_vector` | OK | 384d embedding vector |

#### Vector Quality
- **Dimension:** 384 (matches v2 expectation)
- **Type:** numpy.ndarray
- **Coverage:** 87,488 / 93,675 rows (93%)
- **Missing Vectors:** 6,187 rows (7%) - These rows will need to be filtered or re-embedded

---

## 2. Skill Embeddings Data (`skill_embeddings.json`)

### File Structure
- **Format:** JSON array of skill objects
- **Total Items:** 106 skills
- **Vector Coverage:** 100% (all skills have vectors)

### Schema Analysis

#### Actual Fields
```json
{
  "name": "C",
  "category": "Backend",
  "synonyms": ["C Language", "CLang"],
  "vector": [384 dimensions]
}
```

#### v2 Field Mapping

| v2 Expected Field | Actual Field | Status | Notes |
|-------------------|--------------|--------|-------|
| `skill` | `name` | OK | Skill name (e.g., "C", "Python") |
| `position_category` | `category` | OK | Position category (e.g., "Backend") |
| `skill_vector` | `vector` | OK | 384d embedding vector |

**Additional Field:**
- `synonyms`: String array - Not required by v2 but useful for skill matching

#### Vector Quality
- **Dimension:** 384 (matches v2 expectation)
- **Type:** List of floats
- **Coverage:** 100%

---

## 3. Required Code Adjustments

### Minimal Adjustments Needed

The v2 Python code needs to handle **column name differences** (snake_case vs Title Case). Here are the required mappings:

#### For RecruitData Loading
```python
# In src/domain/recruit/chunk_loader.py or data processing code

COLUMN_MAPPING = {
    'Position': 'position',
    'Company Name': 'company_name',
    'Exp Years': 'experience_years',
    'Primary Keyword': 'primary_keyword',
    'English Level': 'english_level',
    'Published': 'published_at',
    'Long Description': 'long_description',
    'Long Description_lang': 'description_lang',
    'skill_vector': 'skills_vector',  # Just rename
    'skills': 'skills',  # Keep as is
    'id': 'id'  # Keep as is
}

# Apply mapping
df = df.rename(columns=COLUMN_MAPPING)

# Filter rows without vectors
df = df[df['skills_vector'].notna()]
```

#### For SkillEmbeddingDicData Loading
```python
# In src/domain/skill_dic/loader.py

def load_skill_embeddings(file_path: str) -> List[SkillEmbeddingDicData]:
    with open(file_path, 'r', encoding='utf-8') as f:
        skills_raw = json.load(f)

    return [
        SkillEmbeddingDicData(
            skill=item['name'],
            position_category=item['category'],
            skill_vector=item['vector']
        )
        for item in skills_raw
    ]
```

---

## 4. Data Quality Considerations

### Recruitment Data
1. **Missing Vectors (7%)**: 6,187 rows don't have skill_vector
   - **Recommendation:** Filter these out during chunking
   - **Code:** `df = df[df['skill_vector'].notna()]`

2. **Unused Columns**: The following columns exist but aren't used in v2:
   - `__index_level_0__`
   - `embedding_input_text`
   - `embedding_sample`
   - `normalized_skills` (use `skills` instead)

   These can be safely ignored.

3. **Data Type Conversions**:
   - `Published` field may need datetime parsing
   - `Exp Years` may need int conversion
   - `skill_vector` is already numpy.ndarray (ideal for gRPC)

### Skill Embeddings Data
- **100% Complete:** All skills have vectors
- **No Issues:** Direct mapping works perfectly

---

## 5. Integration Checklist

- [x] Verify recruitment data columns match v2 expectations
- [x] Verify skill embeddings fields match v2 expectations
- [x] Confirm vector dimensions are 384d for both datasets
- [x] Identify missing vectors in recruitment data (7%)
- [ ] Implement column renaming in chunk loader
- [ ] Implement null vector filtering in chunk loader
- [ ] Add data validation in domain models
- [ ] Test gRPC streaming with actual data
- [ ] Verify UUID generation doesn't conflict with existing IDs

---

## 6. Performance Notes

### File Sizes
- `recruitment_v1.pkl`: ~500MB (as documented in CLAUDE.md)
- 93,675 rows × 384d vectors = ~140MB of vector data alone

### Memory Optimization Strategy
1. **Don't load entire pkl at once**: Use chunking
2. **Filter early**: Remove null vectors before processing
3. **Stream to Java**: Use client streaming (already implemented)
4. **Default chunk size**: 300 rows (as per current implementation)

---

## Conclusion

**The existing data files are fully compatible with v2 code expectations.**

Required changes:
1. Add column renaming logic in Python data loader
2. Filter out rows with null vectors (7% of data)
3. Ensure proper data type conversions (datetime, int)

No changes needed to:
- Proto definitions
- gRPC streaming logic
- Java entity definitions
- Vector dimensions

**Estimated effort:** 1-2 hours for implementing mappings and testing
