import pandas

if __name__ == "__main__":
    recruit = pandas.read_pickle("data/recruitment_with_embedded.pkl")

    # data_frame은 바로 head() 가능
    df = recruit['data_frame']
    print(df.keys())
    print("--------------")
    print(df.iloc[0][10])
    print("--------------")
    print(df.iloc[0][1])

    # skill_embeddings은 dict일 가능성이 큼
    skills = recruit['skill_embeddings']
    print(skills.shape)  # 앞부분 키 확인


    skill_dic = pandas.read_pickle("data/skill_embeddings_dict.pkl")