.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

Delivery
========

.. note::
   * This section is used to describe a software component packaging.
     For a run-time component this might be executable images, containers, etc.
     For an SDK this might be libraries.

   * This section is typically provided for a platform-component and sdk;
     and referenced in developer and user guides

   * This note must be removed after content has been added.

Example use of a block diagram.

.. blockdiag::


   blockdiag layers {
   orientation = portrait
   a -> m;
   b -> n;
   c -> x;
   m -> y;
   m -> z;
   group l1 {
   color = blue;
   x; y; z;
   }
   group l2 {
   color = yellow;
   m; n;
   }
   group l3 {
   color = orange;
   a; b; c;
   }

   }

