;;; Copyright (C) 2013-2014 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.math)

(defn average [& vals]
  (/
   (reduce + vals)
   (count vals)))

(defn as-percentage [v]
  (* v 100))

(defn ratio->centi-float-precision
  [v]
  (double (with-precision 2 (bigdec v))))
