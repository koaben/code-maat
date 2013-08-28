;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.git
  (:require [instaparse.core :as insta]
            [incanter.core :as incanter]))

;;; This module is responsible for parsing a git log file.
;;;
;;; Input: a log file generated with the following command:
;;;         git log --date=short --stat
;;;
;;; Ouput: An incanter dataset with the following columns:
;;;   :entity :date :author :rev
;;; where
;;;  :entity -> the changed entity as a string
;;;  :date -> commit date as a string
;;;  :author -> as a string
;;;  :rev -> the hash used by git to identify the commit

(def ^:const transform-options
  "Specifies parser transformations."
  {:number read-string})

;;; Here's the instaparse grammar for a git log-file.
;;;
;;; In the current version we only extract basic info on
;;; authors and file modification patterns.
;;; As we add more analysis options (e.g. churn), it gets
;;; interesting to enable more parse output.
(def ^:const grammar
  "
    <S> = entries
    <entries> = (entry <nl*>)* | entry
    entry = commit <nl> author <nl> date <nl> <message> <nl> changes
    commit = <'commit'> <ws> hash
    author = <'Author:'> <ws> #'.+'
    date = <'Date:'> <ws> #'.+'
    message = <nl> <ws> #'.+' <nl>
    changes = change* <summary>
    <change> = <ws*> file <ws> <'|'> <ws> <modification> <nl>
    file = #'[^\\s]+'
    modification = lines_modified <ws> modification_churn
    lines_modified = number
    modification_churn = #'[\\+\\-]+'
    summary = files_changed? <ws*> insertions? <ws*> deletions?
    files_changed = <ws*> number <ws> <files_changed_static>
    files_changed_static = 'file' 's'? ' changed,'
    insertions = number <ws> <'insertion'  's'? '(+)'><','?>
    deletions = number <ws> <'deletion' 's'? '(-)'>
    number = #'\\d+'
    ws = #'\\s+'
    nl = '\\n'
    hash = #'[\\da-f]+'")

(def git-log-parser
  (insta/parser grammar))

(defn as-grammar-map
  [input]
   (git-log-parser input))

;;; The parse result from instaparse is given as hiccup vectors.
;;; We define a set of accessors encapsulating the access to
;;; the individual parts of the associative vectors.
;;; Example input: a seq of =>
;;; [:entry
;;;  [:commit [:hash "123"]]
;;;  [:author "a"]
;;;  [:date "2013-01-30"]
;;;  [:changes
;;;   [:file ...]]]

(defn- rev [z]
  (get-in z [1 1 1]))

(defn- author [z]
  (get-in z [2 1]))

(defn- date [z]
  (get-in z [3 1]))

(defn- changes [z]
  (rest (get-in z [4])))

(defn- files [z]
  (map (fn [[tag name]] name) (changes z)))

(defn- entry-as-row
  [coll z]
  (let [author (author z)
        rev (rev z)
        date (date z)
        files (files z)]
    (reduce conj
            coll
            (for [file files]
              {:author author :rev rev :date date :entity file}))))

(defn grammar-map->rows
  "Transforms the parse result (our grammar map) into
   a seq of maps where each map represents one entity.
   The grammar map is given as nested hiccup vectors."
  [gm]
  (->>
   gm
   (reduce entry-as-row []))) 

(defn parse-log
  [input]
  (->
   input
   as-grammar-map
   grammar-map->rows
   incanter/to-dataset))
