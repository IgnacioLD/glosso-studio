#!/usr/bin/env python3
"""
Regenerates the `ipa` column in English sentence DBs using a proper
word-level G2P tool (eng_to_ipa), so each space-separated token corresponds
to exactly one word of the sentence.

Before: "a j h æ v t u s ɪ s t ɹ̩ z æ n d w ʌ n b ɹ ʌ ð ɹ̩"   (flat Allosaurus)
After:  "aɪ hæv tu ˈsɪstərz ənd wən ˈbrəðər"                    (one token per word)
"""

import sqlite3, re, glob, os
import eng_to_ipa as ipa_lib

PUNCT = re.compile(r"[^\w'-]")


def word_to_ipa(word: str) -> str:
    """Convert a single word to IPA, stripping trailing punctuation first."""
    clean = PUNCT.sub("", word).strip("'-")
    if not clean:
        return ""
    result = ipa_lib.convert(clean)
    # eng_to_ipa returns the word unchanged if unknown — keep it as-is
    return result


def sentence_to_grouped_ipa(text: str) -> str:
    """Return a space-separated IPA string with one token per word."""
    words = text.strip().split()
    groups = [word_to_ipa(w) for w in words]
    return " ".join(g for g in groups if g)


def process_db(db_path: str):
    print(f"Processing {db_path} ...", flush=True)
    con = sqlite3.connect(db_path)
    cur = con.cursor()
    rows = cur.execute("SELECT id, text FROM sentences").fetchall()

    updates = []
    for row_id, text in rows:
        grouped = sentence_to_grouped_ipa(text)
        updates.append((grouped, row_id))

    cur.executemany("UPDATE sentences SET ipa = ? WHERE id = ?", updates)
    con.commit()
    con.close()
    print(f"  Updated {len(updates)} rows.")


if __name__ == "__main__":
    script_dir = os.path.dirname(os.path.abspath(__file__))
    dbs = sorted(glob.glob(os.path.join(script_dir, "sentences_en_*.db")))
    if not dbs:
        print("No sentences_en_*.db found.")
    for db in dbs:
        process_db(db)
    print("\nDone.")
